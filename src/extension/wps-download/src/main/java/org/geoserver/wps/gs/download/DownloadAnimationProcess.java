/*
*==Description==
*GeoServer is an open source software server written in Java that allows users 
*          to share and edit geospatial data.Designed for interoperability, 
*          it publishes data from any major spatial data source using open standards.
*
*Being a community-driven project, GeoServer is developed, tested, and supported by 
*      a diverse group of individuals and organizations from around the world.
*
*GeoServer is the reference implementation of the Open Geospatial Consortium (OGC) 
*          Web Feature Service (WFS) and Web Coverage Service (WCS) standards, as well as
*          a high performance certified compliant Web Map Service (WMS), compliant 
*          Catalog Service for the Web (CSW) and implementing Web Processing Service (WPS).
*          GeoServer forms a core component of the Geospatial Web.
*
*==License==
*GeoServer is distributed under the GNU General Public License Version 2.0 license:
*
*    GeoServer, open geospatial information server
*    Copyright (C) 2014-2020 Open Source Geospatial Foundation.
*    Copyright (C) 2001-2014 OpenPlans
*    
*    This program is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 2 of the License, or
*    (at your option) any later version (collectively, "GPL").
*    
*    As an exception to the terms of the GPL, you may copy, modify,
*    propagate, and distribute a work formed by combining GeoServer with the
*    EMF and XSD Libraries, or a work derivative of such a combination, even if
*    such copying, modification, propagation, or distribution would otherwise
*    violate the terms of the GPL. Nothing in this exception exempts you from
*    complying with the GPL in all respects for all of the code used other
*    than the EMF and XSD Libraries. You may include this exception and its grant
*    of permissions when you distribute GeoServer.  Inclusion of this notice
*    with such a distribution constitutes a grant of such permissions.  If
*    you do not wish to grant these permissions, remove this paragraph from
*    your distribution. "GeoServer" means the GeoServer software licensed
*    under version 2 or any later version of the GPL, or a work based on such
*    software and licensed under the GPL. "EMF and XSD Libraries" means 
*    Eclipse Modeling Framework Project and XML Schema Definition software
*    distributed by the Eclipse Foundation, all licensed 
*    under the Eclipse Public License Version 1.0 ("EPL"), or a work based on 
*    such software and licensed under the EPL.
*    
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*    
*    You should have received a copy of the GNU General Public License
*    along with this program; if not, write to the Free Software
*    Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA 02110-1335  USA
*
*==More Information==
*Visit the website or read the docs.
*/

package org.geoserver.wps.gs.download;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.PlanarImage;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.DateRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.logging.Logging;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;
import org.opengis.util.ProgressListener;

@DescribeProcess(
    title = "Animation Download Process",
    description =
            "Builds an animation given a set of layer "
                    + "definitions, "
                    + "area of interest, size and a series of times for animation frames."
)
public class DownloadAnimationProcess implements GeoServerProcess {

    static final Logger LOGGER = Logging.getLogger(DownloadAnimationProcess.class);
    private static BufferedImage STOP = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);

    public static final String VIDEO_MP4 = "video/mp4";
    private static final Format MAP_FORMAT;

    static {
        MAP_FORMAT = new Format();
        MAP_FORMAT.setName("image/png");
    }

    private final DownloadMapProcess mapper;
    private final WPSResourceManager resourceManager;
    private final DateTimeFormatter formatter;
    private final DownloadServiceConfigurationGenerator confiGenerator;

    public DownloadAnimationProcess(
            DownloadMapProcess mapper,
            WPSResourceManager resourceManager,
            DownloadServiceConfigurationGenerator downloadServiceConfigurationGenerator) {
        this.mapper = mapper;
        this.resourceManager = resourceManager;
        this.confiGenerator = downloadServiceConfigurationGenerator;
        // java 8 formatters are thread safe
        this.formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                        .withLocale(Locale.ENGLISH)
                        .withZone(ZoneId.of("GMT"));
    }

    @DescribeResult(
        name = "result",
        description = "The animation",
        meta = {"mimeTypes=" + VIDEO_MP4, "chosenMimeType=format"}
    )
    public RawData execute(
            @DescribeParameter(
                        name = "bbox",
                        min = 1,
                        description = "The map area and output projection"
                    )
                    ReferencedEnvelope bbox,
            @DescribeParameter(
                        name = "decoration",
                        min = 0,
                        description = "A WMS decoration layout name to watermark" + " the output"
                    )
                    String decorationName,
            @DescribeParameter(name = "headerheight", min = 0, description = "Header height")
                    Integer headerHeight,
            @DescribeParameter(
                        name = "time",
                        min = 1,
                        description =
                                "Map time specification (a range with "
                                        + "periodicity or a list of time values)"
                    )
                    String time,
            @DescribeParameter(name = "width", min = 1, description = "Output width", minValue = 1)
                    int width,
            @DescribeParameter(
                        name = "height",
                        min = 1,
                        description = "Output height",
                        minValue = 1
                    )
                    int height,
            @DescribeParameter(
                        name = "fps",
                        min = 1,
                        description = "Frames per second",
                        minValue = 0,
                        defaultValue = "1"
                    )
                    double fps,
            @DescribeParameter(
                        name = "layer",
                        min = 1,
                        description = "The list of layers",
                        minValue = 1
                    )
                    Layer[] layers,
            ProgressListener progressListener)
            throws Exception {

        // avoid NPE on progress listener, make it effectively final for lambda to use below
        ProgressListener listener =
                Optional.of(progressListener).orElse(new DefaultProgressListener());

        // if height and width are an odd number fix them, cannot encode videos otherwise
        if (width % 2 != 0) {
            width++;
        }
        if (height % 2 != 0) {
            height++;
        }

        final Resource output = resourceManager.getTemporaryResource("mp4");
        Rational frameRate = getFrameRate(fps);

        AWTSequenceEncoder enc =
                new AWTSequenceEncoder(NIOUtils.writableChannel(output.file()), frameRate);

        DownloadServiceConfiguration configuration = confiGenerator.getConfiguration();
        TimeParser timeParser = new TimeParser(configuration.getMaxAnimationFrames());
        Collection parsedTimes = timeParser.parse(time);
        progressListener.started();
        Map<String, WebMapServer> serverCache = new HashMap<>();

        // Have two threads work on encoding. The current thread builds the frames, and submits
        // them into a small queue that the encoder thread picks from
        BlockingQueue<BufferedImage> renderingQueue = new LinkedBlockingDeque<>(1);
        BasicThreadFactory threadFactory =
                new BasicThreadFactory.Builder().namingPattern("animation-encoder-%d").build();
        ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        // a way to get out of the encoding loop in case an exception happens during frame rendering
        AtomicBoolean abortEncoding = new AtomicBoolean(false);
        Future<Void> future =
                executor.submit(
                        () -> {
                            int totalTimes = parsedTimes.size();
                            int count = 1;
                            BufferedImage frame;
                            while ((frame = renderingQueue.take()) != STOP) {
                                enc.encodeImage(frame);
                                listener.progress(90 * (((float) count) / totalTimes));
                                listener.setTask(
                                        new SimpleInternationalString(
                                                "Generated frames "
                                                        + count
                                                        + " out of "
                                                        + totalTimes));
                                count++;
                                // handling exit due to WPS cancellation, or to exceptions
                                if (listener.isCanceled() || abortEncoding.get()) return null;
                            }
                            return null;
                        });
        try {
            for (Object parsedTime : parsedTimes) {
                // turn parsed time into a specification and generate a "WMS" like request based on
                // it
                String mapTime = toWmsTimeSpecification(parsedTime);
                LOGGER.log(Level.FINE, "Building frame for time %s", mapTime);
                RenderedImage image =
                        mapper.buildImage(
                                bbox,
                                decorationName,
                                mapTime,
                                width,
                                height,
                                headerHeight,
                                layers,
                                "image/png",
                                new DefaultProgressListener(),
                                serverCache);
                BufferedImage frame = toBufferedImage(image);
                LOGGER.log(Level.FINE, "Got frame %s", frame);
                renderingQueue.put(frame);

                // exit sooner in case of cancellation, encoding abort is handled in finally
                if (listener.isCanceled()) return null;
            }
            renderingQueue.put(STOP);
            // wait for encoder to finish
            future.get();
            progressListener.progress(100);
        } finally {
            // force encoding thread to stop in case we got here due to an exception
            abortEncoding.set(true);
            executor.shutdown();
        }
        enc.finish();

        return new ResourceRawData(output, VIDEO_MP4, "mp4");
    }

    private BufferedImage toBufferedImage(RenderedImage image) {
        BufferedImage frame;
        if (image instanceof BufferedImage) {
            frame = (BufferedImage) image;
        } else {
            frame = PlanarImage.wrapRenderedImage(image).getAsBufferedImage();
        }
        return frame;
    }

    private String toWmsTimeSpecification(Object parsedTime) {
        String mapTime;
        if (parsedTime instanceof Date) {
            mapTime = formatter.format(((Date) parsedTime).toInstant());
        } else if (parsedTime instanceof DateRange) {
            DateRange range = (DateRange) parsedTime;
            mapTime =
                    formatter.format(range.getMinValue().toInstant())
                            + "/"
                            + formatter.format(range.getMinValue().toInstant());
        } else {
            throw new WPSException("Unexpected parsed date type: " + parsedTime);
        }
        return mapTime;
    }

    private Rational getFrameRate(double fps) {
        if (fps < 0) {
            throw new WPSException("Frames per second must be greater than zero");
        }
        BigDecimal bigDecimal = BigDecimal.valueOf(fps);
        int numerator = (int) bigDecimal.unscaledValue().longValue();
        int denominator = (int) Math.pow(10L, bigDecimal.scale());

        return new Rational(numerator, denominator);
    }
}

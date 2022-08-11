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

package org.geoserver.taskmanager.data.impl;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Identifiable;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.SoftRemove;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.util.InitConfigUtil;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional("tmTransactionManager")
public class TaskManagerDaoImpl implements TaskManagerDao {

    @Autowired private SessionFactory sf;

    protected final Session getSession() {
        Session session = sf.getCurrentSession();
        session.enableFilter("activeTaskFilter");
        session.enableFilter("activeBatchFilter");
        session.enableFilter("activeElementFilter");
        session.enableFilter("activeTaskElementFilter");
        return session;
    }

    protected final Session getSessionNoFilters() {
        Session session = sf.getCurrentSession();
        session.disableFilter("activeTaskFilter");
        session.disableFilter("activeBatchFilter");
        session.disableFilter("activeElementFilter");
        session.disableFilter("activeTaskElementFilter");
        return session;
    }

    @SuppressWarnings("unchecked")
    protected <T> T saveObject(T o) {
        o = (T) getSession().merge(o);
        getSession().flush();
        return o;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Identifiable> T reload(T object) {
        return (T) getSession().get(object.getClass(), object.getId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Identifiable> T lockReload(T object) {
        return (T)
                getSession()
                        .get(
                                object.getClass(),
                                object.getId(),
                                new LockOptions(LockMode.PESSIMISTIC_READ).setScope(true));
    }

    @Override
    public Run save(final Run run) {
        return saveObject(run);
    }

    @Override
    public BatchRun save(final BatchRun br) {
        return saveObject(br);
    }

    @Override
    public Configuration save(final Configuration config) {
        if (Hibernate.isInitialized(config.getBatches())) {
            for (Batch batch : config.getBatches().values()) {
                reorder(batch);
            }
        }
        return initInternal(saveObject(config));
    }

    protected void reorder(Batch batch) {
        if (Hibernate.isInitialized(batch.getElements())) {
            int i = 0;
            for (BatchElement element : batch.getElements()) {
                if (element.isActive()) {
                    element.setIndex(i++);
                } else {
                    element.setIndex(null);
                }
            }
        }
    }

    @Override
    public Batch save(final Batch batch) {
        reorder(batch);
        return initInternal(saveObject(batch));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Batch> getAllBatches() {
        Criteria criteria =
                getSession()
                        .createCriteria(BatchImpl.class)
                        .createAlias("configuration", "configuration", JoinType.LEFT_OUTER_JOIN)
                        .setFetchMode("elements", FetchMode.JOIN)
                        .add(Restrictions.eq("removeStamp", 0L))
                        .add(
                                Restrictions.or(
                                        Restrictions.isNull("configuration"),
                                        Restrictions.eq("configuration.removeStamp", 0L)));
        return criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Batch> getViewableBatches() {
        Criteria criteria =
                getSession()
                        .createCriteria(BatchRunImpl.class, "outerBr")
                        .createAlias("outerBr.batch", "outerBatch")
                        .createAlias(
                                "outerBatch.configuration",
                                "configuration",
                                JoinType.LEFT_OUTER_JOIN)
                        .add(Restrictions.eq("outerBatch.removeStamp", 0L));
        criteria.add(
                Restrictions.or(
                        Restrictions.isNull("outerBatch.configuration"),
                        Restrictions.and(
                                Restrictions.and(
                                        Restrictions.eq("configuration.removeStamp", 0L),
                                        Restrictions.eq("configuration.validated", true)),
                                Restrictions.not(Restrictions.like("outerBatch.name", "@%")))));
        criteria.add(
                Subqueries.propertyEq(
                        "outerBr.id",
                        DetachedCriteria.forClass(RunImpl.class)
                                .createAlias("batchRun", "innerBr")
                                .createAlias("innerBr.batch", "innerBatch")
                                .add(Restrictions.eqProperty("innerBatch.id", "outerBatch.id"))
                                .setProjection(Projections.max("innerBr.id"))));

        for (BatchRunImpl br :
                (List<BatchRunImpl>)
                        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list()) {
            ((BatchImpl) br.getBatch()).setLatestBatchRun(br);
        }

        return getSession()
                .createCriteria(BatchImpl.class)
                .createAlias("configuration", "configuration", JoinType.LEFT_OUTER_JOIN)
                .add(Restrictions.eq("removeStamp", 0L))
                .add(
                        Restrictions.or(
                                Restrictions.isNull("configuration"),
                                Restrictions.and(
                                        Restrictions.and(
                                                Restrictions.eq("configuration.removeStamp", 0L),
                                                Restrictions.eq("configuration.validated", true)),
                                        Restrictions.not(Restrictions.like("name", "@%")))))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadLatestBatchRuns(Configuration config) {
        Criteria criteria =
                getSession()
                        .createCriteria(BatchRunImpl.class, "outerBr")
                        .createAlias("outerBr.batch", "outerBatch")
                        .createAlias(
                                "outerBatch.configuration",
                                "configuration",
                                JoinType.LEFT_OUTER_JOIN)
                        .add(Restrictions.eq("outerBatch.removeStamp", 0L));
        criteria.add(Restrictions.eq("configuration.id", config.getId()));
        criteria.add(
                Subqueries.propertyEq(
                        "outerBr.id",
                        DetachedCriteria.forClass(RunImpl.class)
                                .createAlias("batchRun", "innerBr")
                                .createAlias("innerBr.batch", "innerBatch")
                                .add(Restrictions.eqProperty("innerBatch.id", "outerBatch.id"))
                                .setProjection(Projections.max("innerBr.id"))));

        for (BatchRunImpl br :
                (List<BatchRunImpl>)
                        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list()) {
            BatchImpl b = ((BatchImpl) config.getBatches().get(br.getBatch().getName()));
            if (b != null) {
                b.setLatestBatchRun(br);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Configuration> getConfigurations(Boolean templates) {
        Criteria criteria =
                getSession()
                        .createCriteria(ConfigurationImpl.class)
                        .add(Restrictions.eq("removeStamp", 0L));
        if (templates != null) {
            criteria.add(Restrictions.eq("template", templates));
        }
        return criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
    }

    @Override
    public Configuration getConfiguration(long id) {
        return (Configuration)
                getSession()
                        .createCriteria(ConfigurationImpl.class)
                        .add(Restrictions.idEq(id))
                        .uniqueResult();
    }

    @Override
    public Batch getBatch(long id) {
        return (Batch)
                getSession()
                        .createCriteria(BatchImpl.class)
                        .add(Restrictions.idEq(id))
                        .uniqueResult();
    }

    @Override
    public Configuration getConfiguration(final String name) {
        return (Configuration)
                getSession()
                        .createCriteria(ConfigurationImpl.class)
                        .add(Restrictions.eq("removeStamp", 0L))
                        .add(Restrictions.eq("name", name))
                        .uniqueResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Task> getTasksAvailableForBatch(Batch batch) {
        DetachedCriteria alreadyInBatch =
                DetachedCriteria.forClass(BatchElementImpl.class)
                        .createAlias("batch", "batch")
                        .createAlias("task", "task")
                        .add(Restrictions.eq("batch.id", batch.getId()))
                        .add(Restrictions.eq("removeStamp", 0L))
                        .setProjection(Projections.property("task.id"));
        Criteria criteria =
                getSession()
                        .createCriteria(TaskImpl.class)
                        .createAlias("configuration", "configuration")
                        .createAlias("batchElements", "batchElements", JoinType.LEFT_OUTER_JOIN)
                        .add(Restrictions.eq("removeStamp", 0L))
                        .add(Restrictions.eq("configuration.removeStamp", 0L))
                        .add(Subqueries.propertyNotIn("id", alreadyInBatch));

        if (batch.getConfiguration() == null) {
            criteria.add(Restrictions.eq("configuration.template", false));
        } else {
            criteria.add(Restrictions.eq("configuration.id", batch.getConfiguration().getId()));
        }

        return (List<Task>) criteria.list();
    }

    @Override
    public Batch getBatch(final String fullName) {
        String[] splitName = fullName.split(Batch.FULL_NAME_DIVISOR, 2);
        Criteria criteria =
                getSession()
                        .createCriteria(BatchImpl.class)
                        .add(Restrictions.eq("removeStamp", 0L));

        if (splitName.length > 1) {
            criteria.createAlias("configuration", "configuration")
                    .add(Restrictions.eq("configuration.name", splitName[0]))
                    .add(Restrictions.eq("name", splitName[1]))
                    .add(Restrictions.eq("configuration.removeStamp", 0L));
        } else {
            criteria.add(Restrictions.isNull("configuration"))
                    .add(Restrictions.eq("name", splitName[0]));
        }

        return (Batch) criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Batch> findBatches(
            final String workspacePattern,
            final String configNamePattern,
            final String namePattern) {
        Criteria criteria =
                getSession()
                        .createCriteria(BatchImpl.class)
                        .add(Restrictions.eq("removeStamp", 0L));

        if (configNamePattern != null) {
            criteria.createAlias("configuration", "configuration")
                    .add(Restrictions.like("configuration.name", configNamePattern))
                    .add(Restrictions.like("name", namePattern))
                    .add(Restrictions.eq("configuration.removeStamp", 0L))
                    .add(Restrictions.eq("configuration.template", false))
                    .add(Restrictions.eq("configuration.validated", true))
                    .add(Restrictions.not(Restrictions.like("name", "@%")));
            if (workspacePattern == null) {
                criteria.add(Restrictions.isNull("configuration.workspace"));
            } else if (!"%".equals(workspacePattern)) {
                criteria.add(Restrictions.like("configuration.workspace", workspacePattern));
            }
        } else {
            criteria.add(Restrictions.isNull("configuration"))
                    .add(Restrictions.like("name", namePattern));
            if (workspacePattern == null) {
                criteria.add(Restrictions.isNull("workspace"));
            } else if (!"%".equals(workspacePattern)) {
                criteria.add(Restrictions.like("workspace", workspacePattern));
            }
        }

        return (List<Batch>) criteria.list();
    }

    @Override
    public List<Batch> findInitBatches(String workspacePattern, String configNamePattern) {
        Criteria criteria =
                getSession()
                        .createCriteria(BatchImpl.class)
                        .add(Restrictions.eq("removeStamp", 0L));

        criteria.createAlias("configuration", "configuration")
                .add(Restrictions.like("configuration.name", configNamePattern))
                .add(Restrictions.like("name", InitConfigUtil.INIT_BATCH))
                .add(Restrictions.eq("configuration.removeStamp", 0L))
                .add(Restrictions.eq("configuration.template", false))
                .add(Restrictions.eq("configuration.validated", false));
        if (workspacePattern == null) {
            criteria.add(Restrictions.isNull("configuration.workspace"));
        } else if (!"%".equals(workspacePattern)) {
            criteria.add(Restrictions.like("configuration.workspace", workspacePattern));
        }

        return (List<Batch>) criteria.list();
    }

    @Override
    public BatchElement getBatchElement(final Batch batch, final Task task) {
        return (BatchElement)
                getSession()
                        .createCriteria(BatchElementImpl.class)
                        .createAlias("batch", "batch")
                        .createAlias("task", "task")
                        .add(Restrictions.eq("batch.id", batch.getId()))
                        .add(Restrictions.eq("task.id", task.getId()))
                        .uniqueResult();
    }

    @Override
    public <T extends SoftRemove> T remove(T item) {
        item.setActive(false);
        return saveObject(item);
    }

    @Override
    public Run getCurrentRun(final Task task) {
        return (Run)
                (getSession()
                                .createCriteria(RunImpl.class)
                                .setLockMode(LockMode.PESSIMISTIC_READ)
                                .createAlias("batchElement", "batchElement")
                                .createAlias("batchElement.task", "task")
                                .add(Restrictions.eq("task.id", task.getId()))
                                .add(Restrictions.isNull("end")))
                        .uniqueResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<BatchRun> getCurrentBatchRuns(final Batch batch) {
        return (List<BatchRun>)
                (getSession()
                        .createCriteria(RunImpl.class)
                        .createAlias("batchRun", "batchRun")
                        .createAlias("batchRun.batch", "batch")
                        .add(Restrictions.eq("batch.id", batch.getId()))
                        .add(
                                Restrictions.in(
                                        "status",
                                        (Object[])
                                                new Run.Status[] {
                                                    Run.Status.RUNNING,
                                                    Run.Status.READY_TO_COMMIT,
                                                    Run.Status.COMMITTING
                                                }))
                        .setProjection(Projections.groupProperty("batchRun"))
                        .list());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<BatchRun> getCurrentBatchRuns() {
        return (List<BatchRun>)
                (getSession()
                        .createCriteria(RunImpl.class)
                        .createAlias("batchRun", "batchRun")
                        .createAlias("batchRun.batch", "batch")
                        .add(
                                Restrictions.in(
                                        "status",
                                        (Object[])
                                                new Run.Status[] {
                                                    Run.Status.RUNNING,
                                                    Run.Status.READY_TO_COMMIT,
                                                    Run.Status.COMMITTING
                                                }))
                        .setProjection(Projections.groupProperty("batchRun"))
                        .list());
    }

    @Override
    public BatchRun getBatchRunBySchedulerReference(final String schedulerReference) {
        return (BatchRun)
                (getSession()
                        .createCriteria(BatchRunImpl.class)
                        .add(Restrictions.eq("schedulerReference", schedulerReference))
                        .addOrder(Order.desc("id")) // assuming sequential id generation
                        .setMaxResults(1)
                        .uniqueResult());
    }

    @Override
    public Run getCommittingRun(final Task task) {
        return (Run)
                (getSession()
                                .createCriteria(RunImpl.class)
                                .setLockMode(LockMode.PESSIMISTIC_READ)
                                .createAlias("batchElement", "batchElement")
                                .createAlias("batchElement.task", "task")
                                .add(Restrictions.eq("task.id", task.getId()))
                                .add(Restrictions.isNotNull("end"))
                                .add(Restrictions.eq("status", Run.Status.COMMITTING)))
                        .uniqueResult();
    }

    @Override
    public void delete(Batch batch) {
        batch = (Batch) getSessionNoFilters().get(BatchImpl.class, batch.getId());
        if (batch.getConfiguration() != null) {
            batch.getConfiguration().getBatches().remove(batch.getName());
        }
        getSessionNoFilters().delete(batch);
    }

    @Override
    public void delete(Configuration config) {
        getSessionNoFilters()
                .delete(getSessionNoFilters().get(ConfigurationImpl.class, config.getId()));
    }

    @Override
    public void delete(BatchElement batchElement) {
        batchElement =
                (BatchElement) getSession().get(BatchElementImpl.class, batchElement.getId());
        batchElement.getBatch().getElements().remove(batchElement);
        getSession().delete(batchElement);
    }

    @Override
    public void delete(Task task) {
        task = (Task) getSessionNoFilters().get(TaskImpl.class, task.getId());
        task.getConfiguration().getTasks().remove(task.getName());
        getSessionNoFilters().delete(task);
    }

    @Override
    public Run getLatestRun(BatchElement batchElement) {
        return (Run)
                (getSession()
                                .createCriteria(RunImpl.class)
                                .createAlias("batchElement", "batchElement")
                                .add(Restrictions.eq("batchElement.id", batchElement.getId()))
                                .addOrder(Order.desc("start")))
                        .setMaxResults(1)
                        .uniqueResult();
    }

    @Override
    @Transactional(
        transactionManager = "tmTransactionManager",
        propagation = Propagation.REQUIRES_NEW
    )
    public Configuration copyConfiguration(String configName) {
        ConfigurationImpl clone = (ConfigurationImpl) getConfiguration(configName);
        initInternal(clone);
        getSession().evict(clone);
        clone.setId(null);
        for (Attribute att : clone.getAttributes().values()) {
            att.setConfiguration(clone);
            ((AttributeImpl) att).setId(null);
        }
        for (Task task : clone.getTasks().values()) {
            task.setConfiguration(clone);
            ((TaskImpl) task).setId(null);
            ((TaskImpl) task).setBatchElements(new ArrayList<BatchElement>());
            for (Parameter param : task.getParameters().values()) {
                param.setTask(task);
                ((ParameterImpl) param).setId(null);
            }
        }
        for (Batch batch : clone.getBatches().values()) {
            batch.setConfiguration(clone);
            ((BatchImpl) batch).setId(null);
            for (BatchElement be : batch.getElements()) {
                be.setBatch(batch);
                be.setTask(clone.getTasks().get(be.getTask().getName()));
                ((BatchElementImpl) be).setId(null);
                if (Hibernate.isInitialized(be.getRuns())) {
                    be.getRuns().clear();
                }
                be.getTask().getBatchElements().add(be);
            }
            if (Hibernate.isInitialized(batch.getBatchRuns())) {
                batch.getBatchRuns().clear();
            }
            // disable cloned batches
            if (!clone.isTemplate()) {
                batch.setEnabled(false);
            }
        }
        return clone;
    }

    /**
     * Initialize lazy collection(s) in Batch - not including run history
     *
     * @param be the Batch to be initialized
     * @return return the initialized Batch
     */
    @Override
    public Batch init(Batch b) {
        return initInternal(reload(b));
    }

    /**
     * Initialize lazy collection(s) in Batch - including run history
     *
     * @param be the Batch to be initialized
     * @return return the initialized Batch
     */
    @Override
    public Batch initHistory(Batch b) {
        b = initInternal(reload(b));
        Hibernate.initialize(b.getBatchRuns());
        return b;
    }

    /**
     * Initialize lazy collection(s) in Configuration
     *
     * @param be the Configuration to be initialized
     * @return return the initialized Batch
     */
    @Override
    public Configuration init(Configuration c) {
        return initInternal(reload(c));
    }

    protected Configuration initInternal(Configuration c) {
        Hibernate.initialize(c.getTasks());
        Hibernate.initialize(c.getAttributes());
        Hibernate.initialize(c.getBatches());
        for (Batch b : c.getBatches().values()) {
            Hibernate.initialize(b.getElements());
        }
        for (Task t : c.getTasks().values()) {
            Hibernate.initialize(t.getBatchElements());
            for (BatchElement be : t.getBatchElements()) {
                Hibernate.initialize(be.getBatch().getElements());
            }
        }
        return c;
    }

    protected Batch initInternal(Batch b) {
        Hibernate.initialize(b.getElements());
        for (BatchElement be : b.getElements()) {
            Hibernate.initialize(be.getTask().getBatchElements());
            initInternal(be.getTask().getConfiguration());
        }
        if (b.getConfiguration() != null) {
            initInternal(b.getConfiguration());
        }
        return b;
    }
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.ExecuteUpdateResultCheckStyle;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.relational.TableSpecification;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class EntityBinding {
	private final EntityIdentifier entityIdentifier = new EntityIdentifier( this );
	private EntityDiscriminator entityDiscriminator;
	private SimpleAttributeBinding versionBinding;

	private Entity entity;
	private TableSpecification baseTable;

	private Map<String,AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();

	private Caching caching;

	private Map<String, MetaAttribute> metaAttributes;

	private boolean lazy;
	private boolean mutable;
	private boolean explicitPolymorphism;
	private String whereFilter;
	private String rowId;

	private String discriminatorValue;
	private boolean dynamicUpdate;
	private boolean dynamicInsert;

	private int batchSize;
	private boolean selectBeforeUpdate;
	private int optimisticLockMode;

	private Class entityPersisterClass;
	private Boolean isAbstract;

	private List<String> synchronizedTableNames;

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public TableSpecification getBaseTable() {
		return baseTable;
	}

	public void setBaseTable(TableSpecification baseTable) {
		this.baseTable = baseTable;
	}

	public EntityIdentifier getEntityIdentifier() {
		return entityIdentifier;
	}

	public EntityDiscriminator makeEntityDiscriminator() {
		entityDiscriminator = new EntityDiscriminator( this );
		return entityDiscriminator;
	}

	public EntityDiscriminator getEntityDiscriminator() {
		return entityDiscriminator;
	}

	public SimpleAttributeBinding getVersioningValueBinding() {
		return versionBinding;
	}

	public void setVersioningValueBinding(SimpleAttributeBinding versionBinding) {
		this.versionBinding = versionBinding;
	}

	public Iterable<AttributeBinding> getAttributeBindings() {
		return attributeBindingMap.values();
	}

	public AttributeBinding getAttributeBinding(String name) {
		return attributeBindingMap.get( name );
	}

	public SimpleAttributeBinding makeSimpleAttributeBinding(String name) {
		final SimpleAttributeBinding binding = new SimpleAttributeBinding( this );
		attributeBindingMap.put( name, binding );
		binding.setAttribute( entity.getAttribute( name ) );
		return binding;
	}

	public BagBinding makeBagAttributeBinding(String name) {
		final BagBinding binding = new BagBinding( this );
		attributeBindingMap.put( name, binding );
		binding.setAttribute( entity.getAttribute( name ) );
		return binding;
	}

	public Caching getCaching() {
		return caching;
	}

	public void setCaching(Caching caching) {
		this.caching = caching;
	}

	public Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}

	public void setMetaAttributes(Map<String, MetaAttribute> metaAttributes) {
		this.metaAttributes = metaAttributes;
	}

	public boolean isMutable() {
		return mutable;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public String getWhereFilter() {
		return whereFilter;
	}

	public void setWhereFilter(String whereFilter) {
		this.whereFilter = whereFilter;
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}

	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	public void setDynamicUpdate(boolean dynamicUpdate) {
		this.dynamicUpdate = dynamicUpdate;
	}

	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	public void setDynamicInsert(boolean dynamicInsert) {
		this.dynamicInsert = dynamicInsert;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	public void setSelectBeforeUpdate(Boolean selectBeforeUpdate) {
		this.selectBeforeUpdate = selectBeforeUpdate;
	}

	public int getOptimisticLockMode() {
		return optimisticLockMode;
	}

	public void setOptimisticLockMode(int optimisticLockMode) {
		this.optimisticLockMode = optimisticLockMode;
	}

	public Class getEntityPersisterClass() {
		return entityPersisterClass;
	}

	public void setEntityPersisterClass(Class entityPersisterClass) {
		this.entityPersisterClass = entityPersisterClass;
	}

	public Boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(Boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public void addSynchronizedTable(String tablename) {
		if ( synchronizedTableNames == null ) {
			synchronizedTableNames = new ArrayList<String>();
		}
		synchronizedTableNames.add( tablename );
	}


	// Custom SQL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private String loaderName;

	public String getLoaderName() {
		return loaderName;
	}

	public void setLoaderName(String loaderName) {
		this.loaderName = loaderName;
	}

	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public void setCustomSqlInsert(String sql, boolean callable, ExecuteUpdateResultCheckStyle resultCheckStyle) {
		customInsert = new CustomSQL( sql, callable, resultCheckStyle );
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public void setCustomSqlUpdate(String sql, boolean callable, ExecuteUpdateResultCheckStyle resultCheckStyle) {
		customUpdate = new CustomSQL( sql, callable, resultCheckStyle );
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public void setCustomSqlDelete(String sql, boolean callable, ExecuteUpdateResultCheckStyle resultCheckStyle) {
		customDelete = new CustomSQL( sql, callable, resultCheckStyle );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.sql.results.spi.RowReader;

/**
 * Implementation of ProcedureCallMemento
 *
 * @author Steve Ebersole
 */
public class ProcedureCallMementoImpl implements ProcedureCallMemento {
	private final String procedureName;

	private final ParameterStrategy parameterStrategy;
	private final List<ParameterMemento> parameterDeclarations;

	private final RowReader rowReader;
	private final Set<String> synchronizedQuerySpaces;

	private final Map<String, Object> hintsMap;

	/**
	 * Constructs a ProcedureCallImpl
	 *
	 * @param procedureName The name of the procedure to be called
	 * @param parameterStrategy Are parameters named or positional?
	 * @param parameterDeclarations The parameters registrations
	 * @param synchronizedQuerySpaces Any query spaces to synchronize on execution
	 * @param hintsMap Map of JPA query hints
	 */
	public ProcedureCallMementoImpl(
			String procedureName,
			ParameterStrategy parameterStrategy,
			List<ParameterMemento> parameterDeclarations,
			RowReader rowReader,
			Set<String> synchronizedQuerySpaces,
			Map<String, Object> hintsMap) {
		this.procedureName = procedureName;
		this.parameterStrategy = parameterStrategy;
		this.parameterDeclarations = parameterDeclarations;
		this.rowReader = rowReader;
		this.synchronizedQuerySpaces = synchronizedQuerySpaces;
		this.hintsMap = hintsMap;
	}

	@Override
	public ProcedureCall makeProcedureCall(SharedSessionContractImplementor session) {
		return new ProcedureCallImpl( session, this );
	}

	public String getProcedureName() {
		return procedureName;
	}

	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	public List<ParameterMemento> getParameterDeclarations() {
		return parameterDeclarations;
	}

	public Set<String> getSynchronizedQuerySpaces() {
		return synchronizedQuerySpaces;
	}

	@Override
	public Map<String, Object> getHintsMap() {
		return hintsMap;
	}

	public <R> RowReader<R> getRowReader() {
		return rowReader;
	}

	/**
	 * A "disconnected" copy of the metadata for a parameter, that can be used in ProcedureCallMementoImpl.
	 */
	public static class ParameterMemento {
		private final Integer position;
		private final String name;
		private final ParameterMode mode;
		private final Class javaType;
		private final AllowableParameterType hibernateType;
		private final boolean passNulls;

		/**
		 * Create the memento
		 *
		 * @param position The parameter position
		 * @param name The parameter name
		 * @param mode The parameter mode
		 * @param javaType The Java type of the parameter
		 * @param hibernateType The Hibernate Type.
		 * @param passNulls Should NULL values to passed to the database?
		 */
		public ParameterMemento(
				int position,
				String name,
				ParameterMode mode,
				Class javaType,
				AllowableParameterType hibernateType,
				boolean passNulls) {
			this.position = position;
			this.name = name;
			this.mode = mode;
			this.javaType = javaType;
			this.hibernateType = hibernateType;
			this.passNulls = passNulls;
		}

		public Integer getPosition() {
			return position;
		}

		public String getName() {
			return name;
		}

		public ParameterMode getMode() {
			return mode;
		}

		public Class getJavaType() {
			return javaType;
		}

		public AllowableParameterType getHibernateType() {
			return hibernateType;
		}

		public boolean isPassNullsEnabled() {
			return passNulls;
		}

		/**
		 * Build a ParameterMemento from the given parameter registration
		 *
		 * @param registration The parameter registration from a ProcedureCall
		 *
		 * @return The memento
		 */
		public static ParameterMemento fromRegistration(ParameterRegistrationImplementor registration) {
			return registration.toMemento();
//	^^ 6.0

//	vv 5.3
//			return new ParameterMemento(
//					registration.getPosition(),
//					registration.getName(),
//					registration.getMode(),
//					registration.getParameterType(),
//					registration.getHibernateType(),
//					registration.isPassNullsEnabled()
//			);
		}

	}
}

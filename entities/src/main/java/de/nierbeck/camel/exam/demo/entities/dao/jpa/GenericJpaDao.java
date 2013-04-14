package de.nierbeck.camel.exam.demo.entities.dao.jpa;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import de.nierbeck.camel.exam.demo.entities.dao.GenericDao;


/**
 */
public class GenericJpaDao<T, ID extends Serializable> implements GenericDao<T, ID> {

	protected EntityManager em;

	protected Class<T> entityClass;

	public GenericJpaDao(final Class<T> clazz) {
		entityClass = clazz;
	}

	@Override
	public T findById(ID id, boolean lock) {
		return em.find(entityClass, id);
	}

	@Override
	public List<T> findAll() {
		StringBuilder queryString = new StringBuilder("select o from ");
		queryString.append(entityClass.getSimpleName()).append(" o");
		final Query query = this.em.createQuery(queryString.toString());
		return query.getResultList();
	}

	@Override
	public T makePersistent(T entity) {
		return em.merge(entity);
	}

	@Override
	public void makeTransient(T entity) {
		em.remove(em.merge(entity));
	}

	/**
	 * @param em
	 *            the em to set
	 */
	public void setEm(EntityManager em) {
		this.em = em;
	}

	@Override
	public long countAll() {
		StringBuilder queryString = new StringBuilder("select count(o) from ");
		queryString.append(entityClass.getSimpleName()).append(" o");
		final Query query = this.em.createQuery(queryString.toString());
		return ((Number) query.getSingleResult()).longValue();
	}

}

package de.nierbeck.camel.exam.demo.entities.dao;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Johannes Haase | ISB AG
 *
 */
public interface GenericDao<T, ID extends Serializable> {

	T findById(ID id, boolean lock);

    List<T> findAll();

    T makePersistent(T entity);

    void makeTransient(T entity);

    long countAll();

}

package de.nierbeck.camel.exam.demo.entities.dao.jpa;

import de.nierbeck.camel.exam.demo.entities.CamelMessage;
import de.nierbeck.camel.exam.demo.entities.dao.CamelMessageStoreDao;

public class CamelMessageStoreJpaDao extends GenericJpaDao<CamelMessage, String> implements CamelMessageStoreDao {

	public CamelMessageStoreJpaDao() {
		super(CamelMessage.class);
	}
	
}

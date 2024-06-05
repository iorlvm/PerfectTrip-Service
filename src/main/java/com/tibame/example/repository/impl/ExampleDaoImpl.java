package com.tibame.example.repository.impl;

import com.tibame.example.repository.ExampleDao;
import com.tibame.entity.ExampleEntity;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ExampleDaoImpl implements ExampleDao {
    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional(readOnly = true)
    public ExampleEntity findById(Long id) {
        return sessionFactory.getCurrentSession().get(ExampleEntity.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExampleEntity> findAll() {
        return sessionFactory.getCurrentSession().createQuery("from ExampleEntity", ExampleEntity.class).list();
    }
}
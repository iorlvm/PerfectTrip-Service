package com.tibame.example.repository.impl;

import com.tibame.example.repository.ExampleDao;
import com.tibame.entity.ExampleEntity;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
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

    @Override
    @Transactional(readOnly = true)
    public ExampleEntity findByPhone(String phone) {
        Query<ExampleEntity> query = sessionFactory.getCurrentSession()
                .createQuery("from ExampleEntity where phone = :phone", ExampleEntity.class);
        query.setParameter("phone", phone);
        return query.uniqueResult();
    }

    @Override
    @Transactional
    public boolean create(ExampleEntity user) {
        try {
            sessionFactory.getCurrentSession().save(user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        ExampleEntity entity = findById(id);
        if (entity == null) return false;
        sessionFactory.getCurrentSession().delete(entity);
        return true;
    }

    @Override
    @Transactional
    public boolean update(ExampleEntity exampleEntity) {
        try {
            sessionFactory.getCurrentSession().update(exampleEntity);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
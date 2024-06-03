package com.tibame.repository.impl;

import com.tibame.repository.UserDao;
import com.tibame.entity.User;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class UesrDaoImpl implements UserDao {
    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return sessionFactory.getCurrentSession().get(User.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return sessionFactory.getCurrentSession().createQuery("from User", User.class).list();
    }
}

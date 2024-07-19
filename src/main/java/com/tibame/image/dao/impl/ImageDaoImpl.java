package com.tibame.image.dao.impl;

import com.tibame.entity.Image;
import com.tibame.image.dao.ImageDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ImageDaoImpl implements ImageDao {
    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public Image findById(Long id) {
        Session currentSession = sessionFactory.getCurrentSession();
        return currentSession.get(Image.class, id);
    }

    @Override
    public Image save(Image image) {
        Session currentSession = sessionFactory.getCurrentSession();
        currentSession.saveOrUpdate(image);
        return image;
    }

    @Override
    public void deleteById(Long id) {
        Session currentSession = sessionFactory.getCurrentSession();
        Image image = currentSession.get(Image.class, id);
        if (image != null) {
            currentSession.delete(image);
        }
    }
}

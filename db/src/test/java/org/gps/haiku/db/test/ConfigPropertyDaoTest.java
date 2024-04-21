package org.gps.haiku.db.test;

import org.gps.haiku.db.ConfigPropertyDao;
import org.gps.haiku.db.DbManager;
import org.gps.haiku.db.DbManagerImpl;
import org.gps.haiku.db.model.ConfigProperty;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Created by leogps on 12/08/2018.
 */
@Test
public class ConfigPropertyDaoTest {

    private DbManager dbManager;

    @BeforeClass
    public void init() throws SQLException, ClassNotFoundException {
        dbManager = DbManagerImpl.getInstance();
        String dbName = "haiku_db_" + UUID.randomUUID().toString();
        dbManager.initialize(dbName);
    }

    @Test
    public void insertTest() throws SQLException {
        ConfigPropertyDao configPropertyDao = new ConfigPropertyDao(dbManager.getConnection());

        configPropertyDao.deleteAll();

        ConfigProperty configProperty = new ConfigProperty();
        configProperty.setProperty("font_size");
        configProperty.setValue("24");

        ConfigProperty inserted = configPropertyDao.insert(configProperty);

        Assert.assertEquals(configProperty.getProperty(), inserted.getProperty());
        Assert.assertEquals(configProperty.getValue(), inserted.getValue());

        configPropertyDao.deleteAll();
    }

    @Test
    public void listTest() throws SQLException {
        ConfigPropertyDao configPropertyDao = new ConfigPropertyDao(dbManager.getConnection());

        configPropertyDao.deleteAll();

        {
            ConfigProperty configProperty = new ConfigProperty();
            configProperty.setProperty("font_size");
            configProperty.setValue("24");

            ConfigProperty inserted = configPropertyDao.insert(configProperty);
            Assert.assertNotNull(inserted);
            Assert.assertNotEquals(inserted.getId(), 0);
        }

        {
            ConfigProperty configProperty = new ConfigProperty();
            configProperty.setProperty("ask_for_xml");
            configProperty.setValue("true");

            ConfigProperty inserted = configPropertyDao.insert(configProperty);
            Assert.assertNotNull(inserted);
            Assert.assertNotEquals(inserted.getId(), 0);
        }


        List<ConfigProperty> configPropertyList = configPropertyDao.list();
        Assert.assertTrue(configPropertyList != null);
        Assert.assertTrue(configPropertyList.size() == 2);

        configPropertyDao.deleteAll();
    }

}

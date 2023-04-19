package at.ches.pro.test.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import at.ches.core.chesonline.WebUser;
import at.ches.pro.dao.ISettingsDao;
import at.ches.pro.dao.IWebuserDao;
import at.ches.pro.test.common.AbstractNonTransactionalTest;

public class ConcurrentSettingsDaoTest extends AbstractNonTransactionalTest {

	@Autowired
	private ISettingsDao settingsDao;

	@Autowired
	private IWebuserDao webuserDao;

	/**
	 * This test should check that settings aren't inserted twice, even if the insert method is called concurrently. See ticket #24829.
	 */
	@Test
	public void concurrentSaveWebuserSetting() throws Exception {
		WebUser webuser = new WebUser("test_user", "test_password");
		webuserDao.insert(webuser);

		String key = "test_key";
		String value = "test_value";

		// check that setting doesn't exist
		assertNull(settingsDao.loadWebUserSetting(key, webuser.getId()));

		// insert setting multiple times concurrently
		ExecutorService executorService = Executors.newFixedThreadPool(4);
		for (int i = 0; i < 12; i++) {
			int counter = i;
			executorService.execute(new Runnable() {

				@Override
				public void run() {
					settingsDao.saveWebUserSetting(key, value + counter, webuser.getId());
				}
			});
		}

		// executorService.shutdown();
		// some more comments
		boolean terminated = executorService.awaitTermination(10000, TimeUnit.SECONDS);
		assertTrue(terminated, "Concurrent insertion of webuser setting doesn't terminate.");

		// check that the setting is set after the concurrent insertions
		assertNotNull(settingsDao.loadWebUserSetting(key, webuser.getId()));

		// this method would throw an exception if the setting was inserted twice in the previous concurrent insert calls
		settingsDao.saveWebUserSetting(key, "new value", webuser.getId());

		// assert that the new value has been set
		assertEquals("new value", settingsDao.loadWebUserSetting(key, webuser.getId()));
	}

}

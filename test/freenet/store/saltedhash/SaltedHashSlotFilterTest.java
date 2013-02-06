package freenet.store.saltedhash;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;
import freenet.keys.CHKBlock;
import freenet.keys.CHKDecodeException;
import freenet.keys.CHKEncodeException;
import freenet.keys.CHKVerifyException;
import freenet.keys.ClientCHK;
import freenet.keys.ClientCHKBlock;
import freenet.node.SemiOrderedShutdownHook;
import freenet.store.CHKStore;
import freenet.support.PooledExecutor;
import freenet.support.SimpleReadOnlyArrayBucket;
import freenet.support.Ticker;
import freenet.support.TrivialTicker;
import freenet.support.api.Bucket;
import freenet.support.compress.Compressor;
import freenet.support.io.ArrayBucketFactory;
import freenet.support.io.BucketTools;
import freenet.support.io.FileUtil;

/** Test the slot filter mechanism */
public class SaltedHashSlotFilterTest extends TestCase {
	
	private Random weakPRNG = new Random(12340);
	private PooledExecutor exec = new PooledExecutor();
	private Ticker ticker = new TrivialTicker(exec);
	private File tempDir;

	@Override
	protected void setUp() throws java.lang.Exception {
		tempDir = new File("tmp-cachingfreenetstoretest");
		tempDir.mkdir();
		exec.start();
		SaltedHashFreenetStore.NO_CLEANER_SLEEP = false;
	}

	@Override
	protected void tearDown() {
		FileUtil.removeAll(tempDir);
	}
	
	public void testCHKPresent() throws IOException, CHKEncodeException, CHKVerifyException, CHKDecodeException {
		checkCHKPresent(-1);
		FileUtil.removeAll(tempDir);
		checkCHKPresent(600*1000); // Much longer than the test will take.
	}

	private void checkCHKPresent(int persistenceTime) throws IOException, CHKEncodeException, CHKVerifyException, CHKDecodeException {
		ResizablePersistentIntBuffer.setPersistenceTime(persistenceTime);
		File f = new File(tempDir, "saltstore");
		FileUtil.removeAll(f);

		CHKStore store = new CHKStore();
		SaltedHashFreenetStore<CHKBlock> saltStore = SaltedHashFreenetStore.construct(f, "testCachingFreenetStoreCHK", store, weakPRNG, 500, true, SemiOrderedShutdownHook.get(), true, true, ticker, null);
		saltStore.start(null, true);
		
		int falsePositives = 0;

		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			if(saltStore.probablyInStore(routingKey))
				falsePositives++;
			store.put(block, false);
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		assertTrue(falsePositives <= 5);
		
		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		saltStore.close();
	}
	
	public void testCHKPresentWithClose() throws IOException, CHKEncodeException, CHKVerifyException, CHKDecodeException {
		checkCHKPresentWithClose(-1);
		FileUtil.removeAll(tempDir);
		checkCHKPresentWithClose(600*1000); // Much longer than the test will take.
	}

	public void checkCHKPresentWithClose(int persistenceTime) throws IOException, CHKEncodeException, CHKVerifyException, CHKDecodeException {
		ResizablePersistentIntBuffer.setPersistenceTime(persistenceTime);
		File f = new File(tempDir, "saltstore");
		FileUtil.removeAll(f);

		CHKStore store = new CHKStore();
		SaltedHashFreenetStore<CHKBlock> saltStore = SaltedHashFreenetStore.construct(f, "testCachingFreenetStoreCHK", store, weakPRNG, 500, true, SemiOrderedShutdownHook.get(), true, true, ticker, null);
		saltStore.start(null, true);
		
		int falsePositives = 0;

		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			if(saltStore.probablyInStore(routingKey))
				falsePositives++;
			store.put(block, false);
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		assertTrue(falsePositives <= 5);
		
		saltStore.close();
		store = new CHKStore();
		saltStore = SaltedHashFreenetStore.construct(f, "testCachingFreenetStoreCHK", store, weakPRNG, 500, true, SemiOrderedShutdownHook.get(), true, true, ticker, null);
		saltStore.start(null, true);
		
		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		saltStore.close();
	}
	
	public void testCHKPresentWithAbort() throws IOException, CHKEncodeException, CHKVerifyException, CHKDecodeException {
		int delay = 1000;
		ResizablePersistentIntBuffer.setPersistenceTime(delay);
		File f = new File(tempDir, "saltstore");
		FileUtil.removeAll(f);

		CHKStore store = new CHKStore();
		SaltedHashFreenetStore<CHKBlock> saltStore = SaltedHashFreenetStore.construct(f, "testCachingFreenetStoreCHK", store, weakPRNG, 500, true, SemiOrderedShutdownHook.get(), true, true, ticker, null);
		saltStore.start(null, true);
		
		int falsePositives = 0;

		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			if(saltStore.probablyInStore(routingKey))
				falsePositives++;
			store.put(block, false);
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		assertTrue(falsePositives <= 5);
		
		try {
			Thread.sleep(2*delay);
		} catch (InterruptedException e) {
			// Ignore
		}

		// Abrupt abort. The slots should have been written by now.
		saltStore.close(true);
		store = new CHKStore();
		saltStore = SaltedHashFreenetStore.construct(f, "testCachingFreenetStoreCHK", store, weakPRNG, 500, true, SemiOrderedShutdownHook.get(), true, true, ticker, null);
		saltStore.start(null, true);
		
		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		saltStore.close();
	}
	
	public void testCHKDelayedTurnOnSlotFilters() throws IOException, CHKEncodeException, CHKVerifyException, CHKDecodeException {
		int delay = 1000;
		ResizablePersistentIntBuffer.setPersistenceTime(delay);
		File f = new File(tempDir, "saltstore");
		FileUtil.removeAll(f);

		CHKStore store = new CHKStore();
		SaltedHashFreenetStore<CHKBlock> saltStore = SaltedHashFreenetStore.construct(f, "testCachingFreenetStoreCHK", store, weakPRNG, 500, false, SemiOrderedShutdownHook.get(), true, true, ticker, null);
		saltStore.start(null, true);
		
		int falsePositives = 0;

		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			if(saltStore.probablyInStore(routingKey))
				falsePositives++;
			store.put(block, false);
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		assertTrue(falsePositives == 100);
		
		saltStore.close();
		store = new CHKStore();
		// Now turn on slot filters. Does it still work?
		saltStore = SaltedHashFreenetStore.construct(f, "testCachingFreenetStoreCHK", store, weakPRNG, 500, true, SemiOrderedShutdownHook.get(), true, true, ticker, null);
		saltStore.start(null, true);
		
		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		saltStore.close();
	}
	
	public void testCHKDelayedTurnOnSlotFiltersWithCleaner() throws IOException, CHKEncodeException, CHKVerifyException, CHKDecodeException, InterruptedException {
		int delay = 1000;
		ResizablePersistentIntBuffer.setPersistenceTime(delay);
		File f = new File(tempDir, "saltstore");
		FileUtil.removeAll(f);

		CHKStore store = new CHKStore();
		SaltedHashFreenetStore<CHKBlock> saltStore = SaltedHashFreenetStore.construct(f, "testCachingFreenetStoreCHK", store, weakPRNG, 500, false, SemiOrderedShutdownHook.get(), true, true, ticker, null);
		saltStore.start(null, true);
		
		int falsePositives = 0;

		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			if(saltStore.probablyInStore(routingKey))
				falsePositives++;
			store.put(block, false);
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		assertTrue(falsePositives == 100);
		
		saltStore.close();
		store = new CHKStore();
		// Now turn on slot filters. Does it still work?
		SaltedHashFreenetStore.NO_CLEANER_SLEEP = true;
		saltStore = SaltedHashFreenetStore.construct(f, "testCachingFreenetStoreCHK", store, weakPRNG, 500, true, SemiOrderedShutdownHook.get(), true, true, ticker, null);
		saltStore.start(null, true);
		saltStore.testingWaitForCleanerDone(50, 100);
		
		for(int i=0;i<100;i++) {
			String test = "test" + i;
			ClientCHKBlock block = encodeBlockCHK(test);
			ClientCHK key = block.getClientKey();
			byte[] routingKey = key.getRoutingKey();
			assertTrue(saltStore.probablyInStore(routingKey));
			CHKBlock verify = store.fetch(key.getNodeCHK(), false, false, null);
			String data = decodeBlockCHK(verify, key);
			assertEquals(test, data);
		}
		
		saltStore.close();
	}
	
	private String decodeBlockCHK(CHKBlock verify, ClientCHK key) throws CHKVerifyException, CHKDecodeException, IOException {
		ClientCHKBlock cb = new ClientCHKBlock(verify, key);
		Bucket output = cb.decode(new ArrayBucketFactory(), 32768, false);
		byte[] buf = BucketTools.toByteArray(output);
		return new String(buf, "UTF-8");
	}

	private ClientCHKBlock encodeBlockCHK(String test) throws CHKEncodeException, IOException {
		byte[] data = test.getBytes("UTF-8");
		SimpleReadOnlyArrayBucket bucket = new SimpleReadOnlyArrayBucket(data);
		return ClientCHKBlock.encode(bucket, false, false, (short)-1, bucket.size(), Compressor.DEFAULT_COMPRESSORDESCRIPTOR, false, null, (byte)0);
	}
	


}

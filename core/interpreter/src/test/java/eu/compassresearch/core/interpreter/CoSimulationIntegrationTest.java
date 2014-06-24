package eu.compassresearch.core.interpreter;

import java.io.File;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import eu.compassresearch.core.interpreter.cosim.SubSystem;
import eu.compassresearch.core.interpreter.cosim.communication.Utils;

public class CoSimulationIntegrationTest extends ExternalProcessTest
{

	private static class ProcessInfo
	{
		public final Process process;
		public final IConsoleWatcher finishWatch;

		public ProcessInfo(Process process, IConsoleWatcher finishWatch)
		{
			this.process = process;
			this.finishWatch = finishWatch;
		}
	}

	public ProcessInfo setUpCoordinator(String file, String mainProcess,
			String delegatedProcesses) throws Exception
	{
		return setUpCoordinator(file, mainProcess, delegatedProcesses, new ConsoleWatcher[] {});
	}

	public ProcessInfo setUpCoordinator(String file, String mainProcess,
			String delegatedProcesses, ConsoleWatcher... additionalWatches)
			throws Exception
	{
		Process process = startSecondJVM(CMLJ.class, new String[] { "-process",
				mainProcess, "-delegatedprocessed", delegatedProcesses,
				"-mode", "server", "-cosimport", port.toString(), "-simulate",
				file.replace('/', File.separatorChar) });
		ConsoleWatcher watch = new ConsoleWatcher("coordinator", ConsoleWatcher.FINISHED_MATCH_TEXT);
		ConsoleWatcher listningWatch = new ConsoleWatcher("coordinator", "Waiting for clients...");
		this.watched.add(watch);

		ConsoleWatcher[] watches = new ConsoleWatcher[2 + additionalWatches.length];
		watches[0] = watch;
		watches[1] = listningWatch;
		if (additionalWatches.length > 0)
		{
			int i = 2;
			for (int j = 0; j < additionalWatches.length; j++, i++)
			{
				watches[i] = additionalWatches[j];
			}
		}
		startAutoRead(process, "server", quiet, watches);

		int time = 0;
		final int RETRY_WAIT = 200;
		while (!listningWatch.isMatched())
		{
			if (time * 1000 == DEFAULT_TIMEOUT)
			{
				throw new TimeoutException("Server never started to listen for clients");
			}
			Utils.milliPause(RETRY_WAIT);
			time += RETRY_WAIT;
		}
		return new ProcessInfo(process, watch);

	}

	public ProcessInfo setUpClient(String file, String mainProcess)
			throws Exception
	{

		Process process = startSecondJVM(CMLJ.class, new String[] { "-process",
				mainProcess, "-mode", "client", "-cosimport", port.toString(),
				"-simulate", file.replace('/', File.separatorChar) });
		String name = "client:" + mainProcess;
		ConsoleWatcher watch = new ConsoleWatcher(name, ConsoleWatcher.FINISHED_MATCH_TEXT);
		this.watched.add(watch);
		startAutoRead(process, name, quiet, watch);
		return new ProcessInfo(process, watch);
	}

	public ProcessInfo setUpExternalClient(Class<?> main, String processName,
			String... args) throws Exception
	{

		Process process = startSecondJVM(main, args);
		String name = "client:" + processName;
		ConsoleWatcher watch = new ConsoleWatcher(name, ConsoleWatcher.FINISHED_MATCH_TEXT);
		this.watched.add(watch);
		startAutoRead(process, name, quiet, watch);
		return new ProcessInfo(process, watch);
	}

	protected static class ConsoleWatcher implements IConsoleWatcher
	{
		public static final String FINISHED_MATCH_TEXT = "Simulator status event : FINISHED";

		public boolean matched;

		private String name;

		private final String matchString;

		public ConsoleWatcher(String name, String matchString)
		{
			this.name = name;
			this.matchString = matchString;
		}

		@Override
		public String toString()
		{
			return "Console watcher for: " + name;
		}

		/*
		 * (non-Javadoc)
		 * @see eu.compassresearch.core.interpreter.IConsoleWatcher#match(java.lang.String)
		 */
		@Override
		public void match(String line)
		{
			if (line.contains(matchString))
			{
				matched = true;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see eu.compassresearch.core.interpreter.IConsoleWatcher#isMatched()
		 */
		@Override
		public boolean isMatched()
		{
			return matched;
		}

	}

	@Test
	public void testMain() throws Exception
	{
		String source = "src/test/resources/cosim/main.cml";
		ProcessInfo coordinator = setUpCoordinator(source, "P", "B");
		ProcessInfo client = setUpClient(source, "B");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertTrue("Simulators did not finish successfully", isFinished(coordinator.finishWatch, client.finishWatch));

	}

	@Test
	public void testSkip() throws Exception
	{
		String source = "src/test/resources/cosim/skip.cml";
		ProcessInfo coordinator = setUpCoordinator(source, "P", "A");
		ProcessInfo client = setUpClient(source, "A");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertFalse("Simulators did not finish successfully", isFinished(coordinator.finishWatch, client.finishWatch));

	}

	@Test
	public void testSkipDual() throws Exception
	{
		String source = "src/test/resources/cosim/skip.cml";
		ProcessInfo coordinator = setUpCoordinator(source, "P", "A,B");
		ProcessInfo client = setUpClient(source, "A");
		ProcessInfo client2 = setUpClient(source, "B");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertFalse("Simulators did not finish successfully", isFinished(coordinator.finishWatch, client.finishWatch, client2.finishWatch));

	}

	@Test
	public void testMainTwoClients() throws Exception
	{
		String source = "src/test/resources/cosim/main.cml";
		ProcessInfo coordinator = setUpCoordinator(source, "P", "B,A");
		ProcessInfo client = setUpClient(source, "B");
		ProcessInfo client2 = setUpClient(source, "A");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertTrue("Simulators did not finish successfully", isFinished(coordinator.finishWatch, client.finishWatch, client2.finishWatch));

	}

	@Test
	public void testMainDeadlocked() throws Exception
	{
		String source = "src/test/resources/cosim/main-deadlocked.cml";
		final ConsoleWatcher deadlockedWatch = new ConsoleWatcher("Main", "Simulator status event : DEADLOCKED");
		ProcessInfo coordinator = setUpCoordinator(source, "P", "B", deadlockedWatch);
		setUpClient(source, "B");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertTrue("Simulators did not finish successfully", deadlockedWatch.isMatched());

	}

	@Test
	public void testRW() throws Exception
	{
		String source = "src/test/resources/cosim/RW.cml";
		ProcessInfo coordinator = setUpCoordinator(source, "Main", "Reader");
		ProcessInfo client = setUpClient(source, "Reader");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertFalse("Simulators did not finish successfully", isFinished(coordinator.finishWatch, client.finishWatch));

	}

	@Test
	public void testSyncOnString() throws Exception
	{
		String source = "src/test/resources/cosim/SyncOnString.cml";
		ProcessInfo coordinator = setUpCoordinator(source, "Main", "Writer");
		ProcessInfo client = setUpClient(source, "Writer");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertTrue("Simulators did not finish successfully", isFinished(coordinator.finishWatch, client.finishWatch));

	}

	@Test
	public void testSyncOnRecord() throws Exception
	{
		String source = "src/test/resources/cosim/SyncOnRecord.cml";
		ProcessInfo coordinator = setUpCoordinator(source, "Main", "Writer");
		ProcessInfo client = setUpClient(source, "Writer");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertTrue("Simulators did not deadlock successfully", isFinished(coordinator.finishWatch, client.finishWatch));

	}

	@Test
	public void testExternalAbort() throws Exception
	{
		String source = "src/test/resources/cosim/externalAbort.cml";

		ConsoleWatcher watch = new ConsoleWatcher("coordinator", "The external co-simulation process A aborted with error: 999 execution error");

		ProcessInfo coordinator = setUpCoordinator(source, "P", "A", watch);
		setUpExternalClient(SubSystem.class, "A", new String[] { port.toString() });

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertTrue("Simulators did not abort successfully", watch.isMatched());

	}

	@Test
	public void testTypesQuotes() throws Exception
	{
		String source = "src/test/resources/cosim/TypeTestingSoS.cml";
		ProcessInfo coordinator = setUpCoordinator(source, "NodeSoS", "NodeReaderCS");
		ProcessInfo client = setUpClient(source, "NodeReaderCS");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertTrue("Simulators did not finish successfully", isFinished(coordinator.finishWatch, client.finishWatch));

	}

	@Test
	public void testTypesBool() throws Exception
	{
		String source = "src/test/resources/cosim/TypeTestingSoS.cml";
		ProcessInfo coordinator = setUpCoordinator(source, "BoolSoS", "BoolReaderCS");
		ProcessInfo client = setUpClient(source, "BoolReaderCS");

		waitForCompletion(coordinator.process, DEFAULT_TIMEOUT);

		Assert.assertTrue("Simulators did not finish successfully", isFinished(coordinator.finishWatch, client.finishWatch));

	}

}

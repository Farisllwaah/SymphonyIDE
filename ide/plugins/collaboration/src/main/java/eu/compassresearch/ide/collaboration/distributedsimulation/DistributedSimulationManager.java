package eu.compassresearch.ide.collaboration.distributedsimulation;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.jface.window.Window;

import eu.compassresearch.ide.collaboration.Activator;
import eu.compassresearch.ide.collaboration.communication.ConnectionManager;
import eu.compassresearch.ide.collaboration.communication.messages.RelayMessage;
import eu.compassresearch.ide.collaboration.communication.messages.RelayMessage.RelayType;
import eu.compassresearch.ide.collaboration.communication.messages.SimulationReplyMessage;
import eu.compassresearch.ide.collaboration.communication.messages.SimulationRequestMessage;
import eu.compassresearch.ide.collaboration.communication.messages.SimulationStartMessage;
import eu.compassresearch.ide.collaboration.datamodel.CollaborationGroup;
import eu.compassresearch.ide.collaboration.datamodel.CollaborationProject;
import eu.compassresearch.ide.collaboration.datamodel.User;
import eu.compassresearch.ide.collaboration.ui.menu.CollaborationDialogs;
import eu.compassresearch.ide.collaboration.ui.menu.DistributedSimulationInitialisationDialog;
import eu.compassresearch.ide.collaboration.ui.menu.DistributedSimulationRequestDialog;
import eu.compassresearch.ide.core.resources.ICmlModel;
import eu.compassresearch.ide.core.resources.ICmlProject;
import eu.compassresearch.ide.interpreter.CmlUtil;
import eu.compassresearch.ide.interpreter.ICmlDebugConstants;

public class DistributedSimulationManager
{
	// collaborator to external process mapping
	private Map<String, List<DistributedSimulationConfiguration>> configurations;
	// to update GUI
	private Map<String, List<IDistributedSimulationListener>> listeners;
	private List<RelayServerListener> relayServerClientHandlers;
	private ConnectionManager connectionManager;
	private CollaborationProject projectForCurrentSession;
	private DistributedSimulationInitialisationDialog distSimDlg;
	private boolean do_animation;
	private String serverHostAddress;
	private String selectedTopProcess;
	private int port = 49152;
	
	private User coordinator;
	private RelayServer distRelayServer;
	Map<String,RelayClient> relayToCoordinatorServer;
	
	public DistributedSimulationManager(ConnectionManager connectionManager)
	{
		this.connectionManager = connectionManager;
		configurations = new HashMap<String, List<DistributedSimulationConfiguration>>();
		listeners = new HashMap<String, List<IDistributedSimulationListener>>();
		relayServerClientHandlers = new ArrayList<RelayServerListener>();

		do_animation = false;
	}

	/**
	 * Add a new distributed simulation configuration mapping a collaborator to external processes (Server Side)
	 */
	public void addConfiguration(
			DistributedSimulationConfiguration distributedConfiguration,
			IDistributedSimulationListener notify)
	{
		String collaboratorName = distributedConfiguration.getCollaborator();

		if (!configurations.containsKey(collaboratorName))
		{
			configurations.put(collaboratorName, new ArrayList<DistributedSimulationConfiguration>());
		}
		configurations.get(collaboratorName).add(distributedConfiguration);

		String collaborator = distributedConfiguration.getCollaborator();
		addListener(collaborator, notify);
	}

	/**
	 * Start a new simulation setting, loads the available collaborators and opens the Initiate Simulation dialog
	 * (Server Side)
	 */
	public void initiateDistributedSimulationOnProject(
			CollaborationProject project)
	{
		// clear any previous state
		newSimulationSession();

		// set the project we are working on
		projectForCurrentSession = project;

		String projectWorkspaceName = project.getProjectWorkspaceName();
		List<User> joinedCollaborators = project.getCollaboratorGroup().getJoinedCollaborators();

		// load collaborators
		ArrayList<String> collaborators = new ArrayList<>();
		for (User user : joinedCollaborators)
		{
			collaborators.add(user.getName());
		}

		// only show online collaborators for the distributed simulation
		ConnectionManager connectionManager = Activator.getDefault().getConnectionManager();
		collaborators = connectionManager.retainOnlineCollaborators(collaborators);

		// load processes
		IProject iProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectWorkspaceName);
		if (iProject == null)
		{
			return;
		}
		ICmlProject cmlProj = (ICmlProject) iProject.getAdapter(ICmlProject.class);
		ICmlModel model = cmlProj.getModel();
		List<String> processes = CmlUtil.getGlobalProcessesFromSourceAsString(model);

//TODO
//		if (processes.isEmpty())
//		{
//			CollaborationDialogs.getInstance().displayNotificationPopup("Distributed Simulation", Notification.Dist_Simulation_ERROR_NO_PROCESSES);
//			return;
//		}
//
//		if (collaborators.isEmpty())
//		{
//			CollaborationDialogs.getInstance().displayNotificationPopup("Distributed Simulation", Notification.Dist_Simulation_ERROR_NO_COLLABORATORS);
//			return;
//		}
//
		distSimDlg = CollaborationDialogs.getInstance().getDistributedSimulationInitialisationDialog(processes, collaborators, this);
//
		if (distSimDlg.open() == Window.OK)
		{
			startDistributedSimulation(project);
		}
		
		//launchClientSide(project, "Police", "localhost:" + port);
	}

	/**
	 * Starts the distributed simulation by notifying the other collaborators and launching the debugger. (Server Side)
	 */
	public void startDistributedSimulation(
			CollaborationProject collaborationProject)
	{
		CollaborationGroup collaboratorGroup = collaborationProject.getCollaboratorGroup();
		ID sender = connectionManager.getConnectedUser();
		String uniqueID = collaborationProject.getUniqueID();
		String hostAddress = getServerHostAddress();
		
		relayToCoordinatorServer = new HashMap<String, RelayClient>();

		// build external process list from configurations
		ArrayList<DistributedSimulationConfiguration> configs = getConfigurations();
		StringBuffer externalProcessStrBld = new StringBuffer();
		DistributedSimulationConfiguration config;
		for (Iterator<DistributedSimulationConfiguration> iterator = configs.iterator(); iterator.hasNext();)
		{
			config = iterator.next();
			externalProcessStrBld.append(config.getProcess());
			if (iterator.hasNext())
			{
				externalProcessStrBld.append(",");
			}
		}

		launchSimulatorServerSide(collaborationProject, selectedTopProcess, externalProcessStrBld.toString(), hostAddress);
		

//		//collect collaborators running simulations
//		List<User> collaboratorsInSimulation = new ArrayList<>(); 
		
		// notify collaborators that simulation is starting
		for (Entry<String, List<DistributedSimulationConfiguration>> configEntry : configurations.entrySet())
		{
			User user = collaboratorGroup.getUser(configEntry.getKey());
			if (user != null)
			{
				for (DistributedSimulationConfiguration configuration : configEntry.getValue())
				{
					if (configuration.isApproved())
					{
						//collaboratorsInSimulation.add(user);
						SimulationStartMessage simStartMsg = new SimulationStartMessage(sender, uniqueID, configuration.getProcess());
						connectionManager.sendTo(user, simStartMsg);
					}
				}
			}
		}

		
		//TODO relay stuff
		//create relay client that will connect to server and pass messages to and from the real clients via ECF
//		try
//		{
//			for (User user : collaboratorsInSimulation)
//			{
//				distServerRelay = new RelayClient();
//				distServerRelay.start();	
//			}
//					
//		} catch (UnknownHostException e)
//		{
//			e.printStackTrace();
//		}
	}
	
	public static boolean portAvailable(int port) {
	    if (port < 49152 || port > 65535) {
	        throw new IllegalArgumentException("Invalid start port: " + port);
	    }

	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	    return false;
	}
	
	
	public int getFreePort(){
		int freePort = port;
		do{
			freePort += 1;
		} while (!portAvailable(freePort));
		
		return freePort;
	}

	/**
	 * Launches the debugger. (Server Side)
	 */
	private void launchSimulatorServerSide(
			CollaborationProject collaborationProject, String topProcess,
			String externalProceses, String hostAddress)
	{
		IProject eclipseProject = ResourcesPlugin.getWorkspace().getRoot().getProject(collaborationProject.getProjectWorkspaceName());

		port = getFreePort();
				
		try
		{
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType ctype = launchManager.getLaunchConfigurationType("eu.compassresearch.ide.interpreter.launchConfigurationTypeCoSim");

			ILaunchConfigurationWorkingCopy lcwc = ctype.newInstance(null, launchManager.generateLaunchConfigurationName(collaborationProject.getName()));

			// eclipse project
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_PROJECT, eclipseProject.getName());

			// launch mode
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_IS_ANIMATION, do_animation);
			// is coordinator
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_COSIM_IS_COORDINATOR, true);

			// top process
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_PROCESS_NAME, topProcess);
			// set processes
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_COSIM_EXTERNAL_PROCESSES, externalProceses);

			// set host
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_COSIM_HOST, /* hostAddress */"localhost:" + port);

			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_REMOTE_DEBUG, false);
			
			
			
			// start debugger
			DebugUITools.launch(lcwc, "debug");

		} catch (CoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Updates the status on simulation request in the configuration and GUI. (Server Side)
	 */
	public void simulationRequestUpdate(String collaborator, String projectID,
			String process, boolean accepted)
	{
		if (accepted)
		{
			// approve configuration for collaborator
			List<DistributedSimulationConfiguration> collaboratorConfigs = configurations.get(collaborator);
			for (DistributedSimulationConfiguration config : collaboratorConfigs)
			{
				config.approve(process, collaborator);
			}
		}

		// update UI
		if (listeners.containsKey(collaborator))
		{
			List<IDistributedSimulationListener> notifyList = listeners.get(collaborator);

			for (IDistributedSimulationListener notify : notifyList)
			{
				notify.onRequestUpdated(process, accepted);
			}
		}

		// has all configurations between approved?
		boolean readyForSimulation = true;
		ArrayList<DistributedSimulationConfiguration> configs = getConfigurations();
		for (DistributedSimulationConfiguration config : configs)
		{
			if (!config.isApproved())
			{
				readyForSimulation = false;
				break;
			}
		}

		if (readyForSimulation)
		{
			distSimDlg.readyForSimulation();
		}
	}

	// (Server Side)
	public void setTopProcess(String selectedTopProcess)
	{
		this.selectedTopProcess = selectedTopProcess;
	}

	// (Server Side)
	public void setLaunchMode(boolean animation)
	{
		do_animation = animation;
	}

	/**
	 * Returns all distributed simulation configurations (Server Side)
	 */
	private ArrayList<DistributedSimulationConfiguration> getConfigurations()
	{
		ArrayList<DistributedSimulationConfiguration> configList = new ArrayList<DistributedSimulationConfiguration>();
		for (List<DistributedSimulationConfiguration> configs : configurations.values())
		{
			configList.addAll(configs);
		}

		return configList;
	}

	/**
	 * Sends simulation request to collaborators (Server Side)
	 */
	public void requestSimulationFromCollaborators()
	{
		CollaborationGroup collaboratorGroup = projectForCurrentSession.getCollaboratorGroup();
		String uniqueID = projectForCurrentSession.getUniqueID();
		ID sender = connectionManager.getConnectedUser();
		String serverHostAddress = getServerHostAddress();

		for (Entry<String, List<DistributedSimulationConfiguration>> configEntry : configurations.entrySet())
		{
			User user = collaboratorGroup.getUser(configEntry.getKey());
			if (user != null)
			{
				for (DistributedSimulationConfiguration config : configEntry.getValue())
				{
					SimulationRequestMessage simRequest = new SimulationRequestMessage(sender, uniqueID, config.getProcess(), serverHostAddress);
					connectionManager.sendTo(user, simRequest);
				}
			}
		}
	}

	/**
	 * Opens Simulation Request dialog (Client Side)
	 */
	public void newSimulationRequest(String sender,
			CollaborationProject collaborationProject, String process,
			String remoteHost)
	{
		newSimulationSession();

		DistributedSimulationRequestDialog distributedSimulationRequestDialog = CollaborationDialogs.getInstance().getDistributedSimulationRequestDialog(sender, collaborationProject, process, this);
		
		coordinator = collaborationProject.getCollaboratorGroup().getUser(sender);
		projectForCurrentSession = collaborationProject;
		
		//if ok, start simulation
		if (distributedSimulationRequestDialog.open() == Window.OK)
		{
			launchClientSide(collaborationProject, process, remoteHost);
		}
	}

	/**
	 *  Sends a message to the simulation coordinator that the simulation is approved (Client Side)
	 */
	public void approveSimulation(String requestedByUser,
			CollaborationProject collaborationProject, String process,
			IDistributedSimulationListener notifyOnStart)
	{
		User requestedByuserObj = collaborationProject.getCollaboratorGroup().getUser(requestedByUser);
		final ID sender = connectionManager.getConnectedUser();

		SimulationReplyMessage replyMsg = new SimulationReplyMessage(sender, collaborationProject.getUniqueID(), process, true);
		connectionManager.sendTo(requestedByuserObj, replyMsg);

		addListener(requestedByuserObj.getName(), notifyOnStart);
	}

	/**
	 *  Sends a message to the simulation coordinator that the simulation is declined (Client Side)
	 */
	public void declineSimulation(String user,
			CollaborationProject collaborationProject, String process)
	{
		User userObj = collaborationProject.getCollaboratorGroup().getUser(user);
		final ID sender = connectionManager.getConnectedUser();

		SimulationReplyMessage replyMsg = new SimulationReplyMessage(sender, collaborationProject.getUniqueID(), process, false);

		connectionManager.sendTo(userObj, replyMsg);
	}

	/**
	 *   Notifies GUI that simulation is starting (Client Side)
	 */
	public void simulationStarting(String collaborator, String projectID,
			String process)
	{
		// update UI
		if (listeners.containsKey(collaborator))
		{
			List<IDistributedSimulationListener> notifyList = listeners.get(collaborator);

			for (IDistributedSimulationListener notify : notifyList)
			{
				notify.onSimluationStarting();
			}
		}
	}

	/**
	 *  Launches the debugger. (Client Side)
	 */
	private void launchClientSide(CollaborationProject collaborationProject,
			String topProcess, String remoteHost)
	{
		//start relay server that will pass messages to and from the real server via ECF
		try
		{
			port = getFreePort();
			distRelayServer = new RelayServer(this,port);
			distRelayServer.start();
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}
	
		IProject eclipseProject = ResourcesPlugin.getWorkspace().getRoot().getProject(collaborationProject.getProjectWorkspaceName());

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType ctype = launchManager.getLaunchConfigurationType("eu.compassresearch.ide.interpreter.launchConfigurationTypeCoSim");

		ILaunchConfigurationWorkingCopy lcwc;
		try
		{
			lcwc = ctype.newInstance(null, launchManager.generateLaunchConfigurationName(collaborationProject.getName()));

			// eclipse project
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_PROJECT, eclipseProject.getName());

			// launch mode
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_IS_ANIMATION, false);
			// is coordinator
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_COSIM_IS_COORDINATOR, false);

			// TODO get from GUI
			// top process
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_PROCESS_NAME, topProcess);

			// set host
			lcwc.setAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_COSIM_HOST, /* remoteHost */"localhost:" + port);

			// start debugger
			DebugUITools.launch(lcwc, "debug");

		} catch (CoreException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 *  Add listener for new events in the distributed simulation
	 */
	public void addListener(String collaborator,
			IDistributedSimulationListener notify)
	{
		if (!listeners.containsKey(collaborator))
		{
			listeners.put(collaborator, new ArrayList<IDistributedSimulationListener>());
		}

		listeners.get(collaborator).add(notify);
	}

	/**
	 * Clears configuration and fields in order to start a new session 
	 */
	private void newSimulationSession()
	{
		listeners.clear();
		configurations.clear();
		relayServerClientHandlers.clear();
		distSimDlg = null;
		serverHostAddress = null;
		selectedTopProcess = null;
		coordinator = null;
		relayToCoordinatorServer = null;
	}

	private String getServerHostAddress()
	{
		try
		{
			serverHostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e)
		{
			e.printStackTrace();
		}

		return serverHostAddress;
	}

	/***
	 * Pass received relay message to client
	 * 
	 * Client Side
	 */
	public void passRelayMessageToClient(String data)
	{
		for (RelayServerListener listener : relayServerClientHandlers)
		{
			listener.onReceivedData(data);
		}
	}
	
	/***
	 * Send relay message to coordinator via ECF
	 * 
	 * //Client Side
	 * @param data
	 */
	public void relayMessageToCoordinator(String data)
	{
		final ID sender = connectionManager.getConnectedUser();
		RelayMessage relayMsg = new RelayMessage(sender, projectForCurrentSession.getUniqueID(), data,  RelayType.Client);
		connectionManager.sendTo(coordinator, relayMsg);
	}
	
	/***
	 * Send relay message to client via ECF
	 * 
	 * Server Side
	 */
	public void relayMessageToClient(ID receiverID, String data)
	{
		final ID sender = connectionManager.getConnectedUser();
		RelayMessage relayMsg = new RelayMessage(sender, projectForCurrentSession.getUniqueID(), data,  RelayType.Server);
		connectionManager.sendTo(receiverID, relayMsg);
	}

	/***
	 * Pass received relay message to server on coordinator
	 * 
	 * //Server Side
	 * @param senderID 
	 * @param inputData
	 */
	public void passRelayMessageToCoordinatorServer(ID senderID, String inputData)
	{
		RelayClient relayClient = null;
		
		String senderName = senderID.getName();
		
		//Received a message before?
		if(!relayToCoordinatorServer.containsKey(senderName)){
			
			//if not, create client
			try
			{
				relayClient = new RelayClient(port, senderID, this);
				relayToCoordinatorServer.put(senderName, relayClient);
				relayClient.start();
			
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			
		} else {
			
			//else use existing client
			relayClient = relayToCoordinatorServer.get(senderName);
		}	
		
		relayClient.send(inputData);
	}

	public void addRelayListener(
			RelayServerClientHandler relayServerClientHandler)
	{
		relayServerClientHandlers.add(relayServerClientHandler);
	}
}

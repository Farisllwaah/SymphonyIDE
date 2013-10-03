package eu.compassresearch.ide.interpreter.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.overture.ide.debug.core.dbgp.IDbgpProperty;
import org.overture.ide.debug.core.dbgp.IDbgpStackLevel;

import eu.compassresearch.core.interpreter.api.CmlInterpreterState;
import eu.compassresearch.core.interpreter.debug.Breakpoint;
import eu.compassresearch.core.interpreter.debug.CmlDbgCommandMessage;
import eu.compassresearch.core.interpreter.debug.CmlDbgStatusMessage;
import eu.compassresearch.core.interpreter.debug.CmlDebugCommand;
import eu.compassresearch.core.interpreter.debug.StackFrameDTO;
import eu.compassresearch.core.interpreter.debug.VariableDTO;
import eu.compassresearch.core.interpreter.debug.messaging.CmlRequest;
import eu.compassresearch.core.interpreter.debug.messaging.Message;
import eu.compassresearch.core.interpreter.debug.messaging.MessageCommunicator;
import eu.compassresearch.core.interpreter.debug.messaging.MessageContainer;
import eu.compassresearch.core.interpreter.debug.messaging.RequestMessage;
import eu.compassresearch.core.interpreter.debug.messaging.ResponseMessage;
import eu.compassresearch.ide.interpreter.CmlDebugPlugin;
import eu.compassresearch.ide.interpreter.ICmlDebugConstants;
import eu.compassresearch.ide.interpreter.model.CmlDebugTarget;
import eu.compassresearch.ide.interpreter.model.CmlThread;

public class CmlCommunicationManager extends Thread
{
	private Map<String, MessageEventHandler<RequestMessage>> requestHandlers;
	private Map<String, MessageEventHandler<CmlDbgStatusMessage>> statusHandlers;
	private Map<UUID, ResponseMessage> responses;
	private Object incommingResponseSignal = new Object();
	private BufferedReader fRequestReader;
	final CmlDebugTarget target;

	// socket to communicate with VM
	private Socket fRequestSocket;
	private OutputStream requestOutputStream;

	private CmlThreadManager threadManager;
	private int port;

	public CmlCommunicationManager(
			CmlDebugTarget target,
			CmlThreadManager threadManager,
			Map<String, MessageEventHandler<RequestMessage>> requestHandlers,
			Map<String, MessageEventHandler<CmlDbgStatusMessage>> statusHandlers,
			int port)
	{
		this.target = target;
		this.threadManager = threadManager;
		this.port = port;
		this.requestHandlers = requestHandlers;
		this.statusHandlers = statusHandlers;
		responses = new HashMap<UUID, ResponseMessage>();
	}

	/**
	 * Private methods
	 */
	/**
	 * Sends a command to the running interpreter
	 * 
	 * @param cmd
	 */
	public void sendCommandMessage(CmlDebugCommand cmd)
	{
		CmlDbgCommandMessage message = new CmlDbgCommandMessage(cmd);
		MessageCommunicator.sendMessage(requestOutputStream, message);
	}

	public void sendCommandMessage(CmlDebugCommand cmd, Object content)
	{
		CmlDbgCommandMessage message = new CmlDbgCommandMessage(cmd, content);
		MessageCommunicator.sendMessage(requestOutputStream, message);
	}

	public void sendMessage(Message message)
	{
		MessageCommunicator.sendMessage(requestOutputStream, message);
	}

	public ResponseMessage sendRequestSynchronous(RequestMessage message)
	{
		MessageCommunicator.sendMessage(requestOutputStream, message);
		ResponseMessage responseMessage = null;
		try
		{
			while (responseMessage == null)
			{
				synchronized (this.incommingResponseSignal)
				{

					this.incommingResponseSignal.wait();

					if (responses.containsKey(message.getRequestId()))
						responseMessage = responses.get(message.getRequestId());
				}
			}

		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		return responseMessage;
	}

	/**
	 * Receives a message from the debugger
	 * 
	 * @return The received message wrapped in a MessageContainer
	 * @throws IOException
	 */
	public MessageContainer receiveMessage() throws IOException
	{
		return MessageCommunicator.receiveMessage(fRequestReader, new MessageContainer(new CmlDbgStatusMessage(CmlInterpreterState.TERMINATED_BY_USER)));
	}

	/**
	 * Dispatches the message to the corresponding message handler
	 * 
	 * @param handlers
	 *            The corresponding message handler map for this message type
	 * @param message
	 *            The message to be processed
	 * @return true if the event loop should continue otherwise false
	 */
	private <H extends Message> boolean dispatchMessageHandler(
			Map<String, MessageEventHandler<H>> handlers, H message)
	{
		boolean result = false;

		if (handlers.containsKey(message.getKey()))
		{
			result = handlers.get(message.getKey()).handleMessage(message);
		}

		return result;
	}
//	private <H extends Message> boolean dispatchMessageHandler(final 
//			Map<String, MessageEventHandler<H>> handlers,final H message)
//	{
//		boolean result = false;
//
//		if (handlers.containsKey(message.getKey()))
//		{
//			result =true;
//			new Thread(new Runnable()
//			{
//				
//				@Override
//				public void run()
//				{
//					handlers.get(message.getKey()).handleMessage(message);
//				}
//			}).start();
//			
//		}
//
//		return result;
//	}

	/**
	 * Dispatches the message from messageContainer to the assigned handler of this message type
	 * 
	 * @param messageContainer
	 * @return
	 */
	public boolean processMessage(MessageContainer messageContainer)
	{
		boolean result = false;

		switch (messageContainer.getType())
		{
			case STATUS:
				return dispatchMessageHandler(statusHandlers, (CmlDbgStatusMessage) messageContainer.getMessage());
			case REQUEST:
				return dispatchMessageHandler(requestHandlers, (RequestMessage) messageContainer.getMessage());
			case RESPONSE:
				synchronized (incommingResponseSignal)
				{
					ResponseMessage rm = (ResponseMessage) messageContainer.getMessage();
					responses.put(rm.getRequestId(), rm);
					incommingResponseSignal.notifyAll();
				}
				return true;
			default:
				System.err.println("Unkown message");
				break;
		}

		return result;
	}

	private void connectionClosed()
	{
		try
		{
			this.fRequestSocket.close();
		} catch (IOException e)
		{
			e.printStackTrace();

		}
	}

	@Override
	public void run()
	{
		MessageContainer message = null;
		try
		{
			do
			{
				message = receiveMessage();
			} while (processMessage(message));
		} catch (IOException e)
		{
			CmlDebugPlugin.logError("Error while receving/processing incomming messages from the debugger", e);
		} finally
		{
			connectionClosed();
			threadManager.terminated();
		}
	}

	public boolean isDisconnected()
	{
		return fRequestSocket.isClosed();
	}

	public void connect() throws IOException
	{
		if (waitForConnect(port))
		{
			// initializeHandlers();
			start();
		}
	}

	private boolean waitForConnect(int requestPort) throws IOException
	{
		ServerSocket requestAcceptor = null;
		try
		{
			requestAcceptor = new ServerSocket(requestPort);
			// FIXME change to config
			int timeout = 5000;
			if (target.getLaunch().getLaunchConfiguration().getAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_REMOTE_DEBUG, false))
			{
				timeout = 30000;
			}
			requestAcceptor.setSoTimeout(timeout);

			fRequestSocket = requestAcceptor.accept();
			requestOutputStream = fRequestSocket.getOutputStream();
			fRequestReader = new BufferedReader(new InputStreamReader(fRequestSocket.getInputStream()));
			return true;
		} catch (CoreException e)
		{
			CmlDebugPlugin.logError("Failed to obtain remote debug flag from launch config", e);
		} finally
		{
			if (requestAcceptor != null)
				requestAcceptor.close();
		}
		return false;
	}

	public void terminate()
	{
		if (fRequestSocket != null)
		{
			synchronized (fRequestSocket)
			{
				sendCommandMessage(CmlDebugCommand.STOP);
			}
		}

	}

	public void disconnect()
	{
		sendCommandMessage(CmlDebugCommand.DISCONNECT);
	}

	public boolean isConnected()
	{
		return (fRequestSocket == null ? false : !fRequestSocket.isClosed());
	}

	public void addBreakpoint(URI file, int linenumber, boolean enabled)
	{
		if (enabled)
		{
			Breakpoint cmlBP = new Breakpoint(System.identityHashCode(file.toString()
					+ linenumber), file, linenumber);
//			System.out.println("Debug target: " + cmlBP);
			this.sendCommandMessage(CmlDebugCommand.SET_BREAKPOINT, cmlBP);
		}
	}

	public void removeBreakpoint(URI file, int linenumber)
	{
		Breakpoint cmlBP = new Breakpoint(System.identityHashCode(file.toString()
				+ linenumber), file, linenumber);
//		System.out.println("Debug target: " + cmlBP);
		this.sendCommandMessage(CmlDebugCommand.REMOVE_BREAKPOINT, cmlBP);
	}

	public void updateBreakpoint(URI file, int linenumber, boolean enabled)
	{

	}

	public IDbgpStackLevel[] getStackLevels(CmlThread thread)
	{
		List<IDbgpStackLevel> levels = new Vector<IDbgpStackLevel>();

		ResponseMessage rm = this.sendRequestSynchronous(new RequestMessage(CmlRequest.GET_STACK_FRAMES, thread.id));

		List<StackFrameDTO> stackFrames = rm.getContent();

		for (StackFrameDTO frame : stackFrames)
		{
			levels.add(new CmlStackLevel(frame.getFile(), "unknown", frame.getLevel(), frame.getLineNumber(), 1, 1));
		}

		// ICmlProject p = ((CmlDebugTarget) thread.getDebugTarget()).project;
		//
		// try
		// {
		// // for (ICmlSourceUnit su : p.getSourceUnits())
		// // {
		// ICmlSourceUnit su = p.getSourceUnits().get(0);
		// levels.add(new CmlStackLevel(su.getFile().getLocationURI(), "A -"
		// + su.getFile().getName(), 1, 1, 1, 1));
		// levels.add(new CmlStackLevel(su.getFile().getLocationURI(), "B -"
		// + su.getFile().getName(), 2, 2, 1, 1));
		// levels.add(new CmlStackLevel(su.getFile().getLocationURI(), "C -"
		// + su.getFile().getName(), 3, 3, 1, 1));
		// // }
		// } catch (CoreException e)
		// {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		return levels.toArray(new IDbgpStackLevel[] {});
	}

	private static IDbgpProperty[] getChilds()
	{
		List<IDbgpProperty> properties = new Vector<IDbgpProperty>();
		properties.add(new DbgpProperty("a1", "A`a", "int", "1", 0, false, false, "A`a", "", new IDbgpProperty[] {}, 0, 0));
		properties.add(new DbgpProperty("a2", "A`a", "int", "2", 0, false, false, "A`a", "", new IDbgpProperty[] {}, 0, 0));
		return properties.toArray(new IDbgpProperty[properties.size()]);
	}

	public IDbgpProperty[] getContextProperties(int threadId, int level,
			int contextId2)
	{
		
		ResponseMessage rm = this.sendRequestSynchronous(new RequestMessage(CmlRequest.GET_CONTEXT_PROPERTIES, new int[]{threadId,level}));

		List<VariableDTO> variables = rm.getContent();

		List<IDbgpProperty> properties = new Vector<IDbgpProperty>();
		
		for (VariableDTO var : variables)
		{
			properties.add(convert(var));
		}
//		switch (level)
//		{
//			case 1:
//				properties.add(new DbgpProperty("a" + threadId, "A`a", "int", "8", 2, true, false, "A`a", "", getChilds(), 0, 0));
//				properties.add(new DbgpProperty("b" + threadId, "A`a", "int", "8", 0, false, false, "A`a", "", new IDbgpProperty[] {}, 0, 0));
//				break;
//			case 2:
//				properties.add(new DbgpProperty("c" + threadId, "A`a", "int", "8", 0, false, false, "A`a", "", new IDbgpProperty[] {}, 0, 0));
//				properties.add(new DbgpProperty("d" + threadId, "A`a", "int", "8", 0, false, false, "A`a", "", new IDbgpProperty[] {}, 0, 0));
//				break;
//			case 3:
//				properties.add(new DbgpProperty("e" + threadId, "A`a", "int", "8", 0, false, false, "A`a", "", new IDbgpProperty[] {}, 0, 0));
//				properties.add(new DbgpProperty("f" + threadId, "A`a", "int", "8", 0, false, false, "A`a", "", new IDbgpProperty[] {}, 0, 0));
//				properties.add(new DbgpProperty("g" + threadId, "A`a", "int", "8", 0, false, false, "A`a", "", new IDbgpProperty[] {}, 0, 0));
//				break;
//		}

		return properties.toArray(new IDbgpProperty[properties.size()]);
	}
	
	private IDbgpProperty convert(VariableDTO var)
	{
		List<IDbgpProperty> children = new Vector<IDbgpProperty>();
		
		if(var.hasChildren())
		{
		for (VariableDTO child : var.getAvailableChildren())
		{
			children.add(convert(child));
		}
		}
		
		return new DbgpProperty(var.getName(), var.getFullName(), var.getType(),var.getValue(), var.getChildrenCount(),var.hasChildren(), var.isConstant(), var.getFullName(), "", children.toArray(new IDbgpProperty[children.size()]), 100, 100);
	}

}

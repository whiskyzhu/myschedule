package myschedule.service;

import static myschedule.service.ErrorCode.SCHEDULER_SERIVCE_NOT_FOUND;

import javax.servlet.http.HttpSession;

import myschedule.web.SessionData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide search and storing of SchedulerService instance in a web application env.
 *
 * @author Zemian Deng
 */
public class SchedulerServiceFinder {
	
	public static final String SESSION_DATA_KEY = "sessionData";

	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	protected SchedulerService defaultSchedulerService;
	
	protected SchedulerServiceContainer schedulerServiceContainer;
	
	public void setDefaultSchedulerService(SchedulerService defaultSchedulerService) {
		this.defaultSchedulerService = defaultSchedulerService;
	}
	
	public SchedulerService getDefaultSchedulerService() {
		return defaultSchedulerService;
	}
	
	public void setSchedulerServiceContainer(SchedulerServiceContainer schedulerServiceContainer) {
		this.schedulerServiceContainer = schedulerServiceContainer;
	}
	
	public SchedulerService find(HttpSession session) {
		SessionData data = getOrCreateSessionData(session);
		SchedulerService result = findBySessionData(data);
		if (result == null) {
			// Not found, try the default scheduler.
			if (defaultSchedulerService == null)
				throw new ErrorCodeException(SCHEDULER_SERIVCE_NOT_FOUND, "Unable to find any scheduler service.");
			
			// We have a default scheduler
			result = defaultSchedulerService;
			logger.debug("Using default scheduler service.");
			
			// Need to update session with default scheduler.
			data.setCurrentSchedulerName(result.getName());
			logger.info("Added default scheduler service "+ result.getName() + " into session data.");
		}
		
		logger.debug("Found scheduler service: " + result.getName());
		return result;
	}
	
	private SchedulerService findBySessionData(SessionData data) {
		String name = data.getCurrentSchedulerName();
		if (name == null) {
			return null;
		}
		
		SchedulerService schedulerService = null;
		
		// We might have multiple threads access this part of code from controller.
		synchronized(schedulerServiceContainer) {
			if (schedulerServiceContainer.hasSchedulerService(name)) {
				schedulerService = schedulerServiceContainer.getSchedulerService(name);
			}
		}
		return schedulerService;
	}

	protected SessionData createSessionData() {
		SessionData data = new SessionData();
		logger.info("New session data created: " + data);
		return data;
	}
	
	protected boolean hasSessionData(HttpSession session) {
		return session.getAttribute(SESSION_DATA_KEY) != null;
	}
	
	public SessionData getOrCreateSessionData(HttpSession session) {
		SessionData data = (SessionData)session.getAttribute(SESSION_DATA_KEY);
		if (data == null) {
			data = createSessionData();
			setSessionData(session, data);
		}
		return data;
	}
	
	protected void setSessionData(HttpSession session, SessionData data) {
		session.setAttribute(SESSION_DATA_KEY, data);
	}
	
	public SchedulerService switchSchedulerService(String newSchedulerName, HttpSession session) {
		SessionData data = getOrCreateSessionData(session);
		String currentSchedulerName = data.getCurrentSchedulerName();
		SchedulerService schedulerService = schedulerServiceContainer.getSchedulerService(newSchedulerName);
		data.setCurrentSchedulerName(newSchedulerName);
		setSessionData(session, data);
		logger.info("Switched scheduler service in session data from " + 
				currentSchedulerName + " to " + newSchedulerName);
		return schedulerService;
	}
}
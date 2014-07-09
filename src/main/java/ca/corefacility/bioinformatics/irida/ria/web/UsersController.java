package ca.corefacility.bioinformatics.irida.ria.web;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.corefacility.bioinformatics.irida.exceptions.EntityNotFoundException;
import ca.corefacility.bioinformatics.irida.model.Project;
import ca.corefacility.bioinformatics.irida.model.enums.ProjectRole;
import ca.corefacility.bioinformatics.irida.model.joins.Join;
import ca.corefacility.bioinformatics.irida.model.user.User;
import ca.corefacility.bioinformatics.irida.ria.utilities.DataTable;
import ca.corefacility.bioinformatics.irida.ria.utilities.Formats;
import ca.corefacility.bioinformatics.irida.service.user.UserService;

import com.google.common.collect.Lists;

/**
 * Controller for all {@link User} related views
 * 
 * @author Josh Adam <josh.adam@phac-aspc.gc.ca>
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 */
@Controller
@RequestMapping(value = "/users")
public class UsersController {
	private static final String USERS_PAGE = "user/list";
	private static final String SPECIFIC_USER_PAGE = "user/user_details";
	private static final String ERROR_PAGE = "error";
	private static final String SORT_BY_ID = "id";
	private static final String SORT_ASCENDING = "asc";
	private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

	private final UserService userService;

	private final List<String> SORT_COLUMNS = Lists.newArrayList(SORT_BY_ID, "username", "email", "lastName",
			"firstName", "systemRole", "createdDate", "modifiedDate");

	private MessageSource messageSource;

	@Autowired
	public UsersController(UserService userService, MessageSource messageSource) {
		this.userService = userService;
		this.messageSource = messageSource;
	}

	/**
	 * Request for the page to display a list of all projects available to the
	 * currently logged in user.
	 * 
	 * @return The name of the page.
	 */
	@RequestMapping
	public String getUsersPage() {
		return USERS_PAGE;
	}

	/**
	 * Request for a specific project details page.
	 * 
	 * @param userId
	 *            The id for the project to show details for.
	 * @param model
	 *            Spring model to populate the html page.
	 * @return The name of the project details page.
	 */

	@RequestMapping(value = "/{userId}")
	public String getProjectSpecificPage(@PathVariable Long userId, final Model model) {
		logger.debug("Getting project information for [User " + userId + "]");
		String page;
		try {
			User read = userService.read(userId);
			model.addAttribute("user", read);
			page = SPECIFIC_USER_PAGE;
		} catch (EntityNotFoundException e) {
			// TODO: (Josh - 2014-06-24) Format error page if project is not
			// found. These should probably be redirects.
			page = ERROR_PAGE;
		} catch (AccessDeniedException e) {
			// TODO: (Josh - 2014-06-24) Format error page if user does not have
			// access. These should probably be redirects.
			page = ERROR_PAGE;
		}
		return page;
	}

	/**
	 * Handles AJAX request for getting a list of projects available to the
	 * logged in user. Produces JSON.
	 * 
	 * @param principal
	 *            The currently logged in user.
	 * @param request
	 *            Contains the parameters for the datatable.
	 * @return JSON value of the projects.
	 */
	@RequestMapping(value = "/ajax/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, Object> getAjaxProjectList(final Principal principal,
			@RequestParam(DataTable.REQUEST_PARAM_START) Integer start,
			@RequestParam(DataTable.REQUEST_PARAM_LENGTH) Integer length,
			@RequestParam(DataTable.REQUEST_PARAM_DRAW) Integer draw,
			@RequestParam(value = DataTable.REQUEST_PARAM_SORT_COLUMN, defaultValue = "0") Integer sortColumn,
			@RequestParam(value = DataTable.REQUEST_PARAM_SORT_DIRECTION, defaultValue = "asc") String direction,
			@RequestParam(DataTable.REQUEST_PARAM_SEARCH_VALUE) String searchValue) {

		String sortString;

		try {
			sortString = SORT_COLUMNS.get(sortColumn);
		} catch (IndexOutOfBoundsException ex) {
			sortString = SORT_BY_ID;
		}

		Sort.Direction sortDirection = direction.equals(SORT_ASCENDING) ? Sort.Direction.ASC : Sort.Direction.DESC;

		int pageNum = start / length;

		Page<User> userPage = userService.searchUser(searchValue, pageNum, length, sortDirection, sortString);

		Locale locale = LocaleContextHolder.getLocale();
		List<List<String>> usersData = new ArrayList<>();
		for (User user : userPage) {
			//getting internationalized system role from the message source
			String roleMessageName = "systemrole." + user.getSystemRole().getName();
			String systemRole = messageSource.getMessage(roleMessageName, null, locale);
			
			List<String> row = new ArrayList<>();
			row.add(user.getId().toString());
			row.add(user.getUsername());
			row.add(user.getLastName());
			row.add(user.getFirstName());
			row.add(user.getEmail());
			row.add(systemRole);
			row.add(Formats.DATE.format(user.getCreatedDate()));
			row.add(Formats.DATE.format(user.getModifiedDate()));
			usersData.add(row);
		}

		Map<String, Object> map = new HashMap<>();
		map.put(DataTable.RESPONSE_PARAM_DRAW, draw);
		map.put(DataTable.RESPONSE_PARAM_RECORDS_TOTAL, userPage.getTotalElements());
		map.put(DataTable.RESPONSE_PARAM_RECORDS_FILTERED, userPage.getTotalElements());

		map.put(DataTable.RESPONSE_PARAM_DATA, usersData);
		return map;
	}

}

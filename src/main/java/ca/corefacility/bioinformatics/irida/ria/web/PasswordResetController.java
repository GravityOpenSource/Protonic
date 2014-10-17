package ca.corefacility.bioinformatics.irida.ria.web;

import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.corefacility.bioinformatics.irida.exceptions.EntityNotFoundException;
import ca.corefacility.bioinformatics.irida.model.user.PasswordReset;
import ca.corefacility.bioinformatics.irida.model.user.Role;
import ca.corefacility.bioinformatics.irida.model.user.User;
import ca.corefacility.bioinformatics.irida.ria.utilities.EmailController;
import ca.corefacility.bioinformatics.irida.service.user.PasswordResetService;
import ca.corefacility.bioinformatics.irida.service.user.UserService;

import com.google.common.collect.ImmutableList;

/**
 * Controller for handling password reset flow
 * 
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 *
 */
@Controller
@RequestMapping(value = "/password_reset")
public class PasswordResetController {
	private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);
	public static final String PASSWORD_RESET_PAGE = "password/password_reset";
	public static final String PASSWORD_RESET_SUCCESS = "password/password_reset_success";
	public static final String CREATE_RESET_PAGE = "password/create_password_reset";
	public static final String RESET_CREATED_PAGE = "password/reset_created";
	public static final String ACTIVATION_PAGE = "password/activate";
	public static final String SUCCESS_REDIRECT = "redirect:/password_reset/success/";
	public static final String CREATED_REDIRECT = "redirect:/password_reset/created/";

	private final UserService userService;
	private final PasswordResetService passwordResetService;
	private final EmailController emailController;
	private final MessageSource messageSource;

	@Autowired
	public PasswordResetController(UserService userService, PasswordResetService passwordResetService,
			EmailController emailController, MessageSource messageSource) {
		this.userService = userService;
		this.passwordResetService = passwordResetService;
		this.emailController = emailController;
		this.messageSource = messageSource;
	}

	/**
	 * Get the password reset page
	 * 
	 * @param resetId
	 *            The ID of the {@link PasswordReset}
	 * @param model
	 *            A model for the page
	 * @return The string name of the page
	 */
	@RequestMapping(value = "/{resetId}", method = RequestMethod.GET)
	public String getResetPage(@PathVariable String resetId,
			@RequestParam(required = false, defaultValue = "false") boolean expired, Model model) {
		setAuthentication();

		PasswordReset passwordReset = passwordResetService.read(resetId);
		User user = passwordReset.getUser();

		model.addAttribute("user", user);
		model.addAttribute("passwordReset", passwordReset);
		if (expired) {
			model.addAttribute("expired", true);
		}

		if (!model.containsAttribute("errors")) {
			model.addAttribute("errors", new HashMap<>());
		}

		return PASSWORD_RESET_PAGE;
	}

	/**
	 * Send the new password for a given password reset
	 * 
	 * @param resetId
	 *            The ID of the {@link PasswordReset}
	 * @param password
	 *            The new password to set
	 * @param confirmPassword
	 *            Confirm the new password
	 * @param model
	 *            A model for the given page
	 * @param locale
	 *            The locale of the request
	 * @return The string name of the success view, or on failure the
	 *         getResetPage view
	 */
	@RequestMapping(value = "/{resetId}", method = RequestMethod.POST)
	public String sendNewPassword(@PathVariable String resetId, @RequestParam String password,
			@RequestParam String confirmPassword, Model model, Locale locale) {
		setAuthentication();
		Map<String, String> errors = new HashMap<>();

		// read the reset to verify it exists first
		PasswordReset passwordReset = passwordResetService.read(resetId);
		User user = passwordReset.getUser();

		if (!password.equals(confirmPassword)) {
			errors.put("password", messageSource.getMessage("user.edit.password.match", null, locale));
		}

		if (errors.isEmpty()) {
			// Set the user's authentication to update the password and log them
			// in
			Authentication token = new UsernamePasswordAuthenticationToken(user, password, ImmutableList.of(user
					.getSystemRole()));
			SecurityContextHolder.getContext().setAuthentication(token);

			try {
				userService.changePassword(user.getId(), password);
			} catch (ConstraintViolationException ex) {
				Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();

				for (ConstraintViolation<?> violation : constraintViolations) {
					logger.debug(violation.getMessage());
					String errorKey = violation.getPropertyPath().toString();
					errors.put(errorKey, violation.getMessage());
				}
			}
		}

		if (!errors.isEmpty()) {
			model.addAttribute("errors", errors);
			return getResetPage(resetId, false, model);
		} else {
			passwordResetService.delete(resetId);
			String email = Base64.getEncoder().encodeToString(user.getEmail().getBytes());
			return SUCCESS_REDIRECT + email;
		}
	}

	/**
	 * Success page for a password reset
	 * 
	 * @param encodedEmail
	 *            A base64 encoded email address
	 * @param model
	 *            Model for the view
	 * @return The password reset success view name
	 */
	@RequestMapping("/success/{encodedEmail}")
	public String resetSuccess(@PathVariable String encodedEmail, Model model) {
		byte[] decode = Base64.getDecoder().decode(encodedEmail);
		String email = new String(decode);
		logger.debug("Password reset submitted for " + email);

		// Authentication should not need to be set at this point, as the user
		// will be logged in
		User user = userService.loadUserByEmail(email);
		model.addAttribute("user", user);
		return PASSWORD_RESET_SUCCESS;
	}

	/**
	 * Get the reset password page
	 * 
	 * @param model
	 *            Model for this view
	 * @return The view name for the email entry page
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String noLoginResetPassword(Model model) {
		return CREATE_RESET_PAGE;
	}

	/**
	 * Create a password reset for the given email address
	 * 
	 * @param email
	 *            The email address to create a password reset for
	 * @param model
	 *            Model for the view
	 * @return Reset created page if the email exists in the system
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String submitEmail(@RequestParam String email, Model model) {
		setAuthentication();
		String page;

		model.addAttribute("email", email);

		try {
			User user = userService.loadUserByEmail(email);

			createNewPasswordReset(user);

			page = CREATED_REDIRECT + Base64.getEncoder().encodeToString(email.getBytes());
		} catch (EntityNotFoundException ex) {
			model.addAttribute("emailError", true);
			SecurityContextHolder.clearContext();
			page = noLoginResetPassword(model);
		}

		return page;
	}

	/**
	 * Success page for creating a password reset
	 * 
	 * @param encodedEmail
	 *            Base64 encoded email of the user
	 * @param model
	 *            Model for the request
	 * @return View name for the reset created page
	 */
	@RequestMapping("/created/{encodedEmail}")
	public String resetCreatedSuccess(@PathVariable String encodedEmail, Model model) {
		// decode the email
		byte[] decode = Base64.getDecoder().decode(encodedEmail);
		String email = new String(decode);

		model.addAttribute("email", email);

		return RESET_CREATED_PAGE;
	}

	/**
	 * Return the activation view
	 * 
	 * @param model
	 *            Model for the view
	 * @return Name of the activation view
	 */
	@RequestMapping(value = "/activate", method = RequestMethod.GET)
	public String activate(Model model) {
		return ACTIVATION_PAGE;
	}

	@RequestMapping(value = "/activate", method = RequestMethod.POST)
	public String getPasswordReset(@RequestParam String activationId, Model model) {
		return "redirect:/password_reset/" + activationId;
	}

	/**
	 * Create a new {@link PasswordReset} for the given {@link User}
	 * 
	 * @param userId
	 *            The ID of the {@link User}
	 */
	@RequestMapping("/ajax/create/{userId}")
	@ResponseBody
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public void adminNewPasswordReset(@PathVariable Long userId) {
		User user = userService.read(userId);
		createNewPasswordReset(user);
	}

	/**
	 * Create a new password reset for a given {@link User} and send a reset
	 * email
	 * 
	 * @param user
	 *            The user to create the reset for
	 */
	private void createNewPasswordReset(User user) {
		PasswordReset passwordReset = new PasswordReset(user);
		passwordResetService.create(passwordReset);

		// email the user their info
		emailController.sendPasswordResetLinkEmail(user, passwordReset);
	}

	/**
	 * Set an anonymous authentication token
	 */
	private void setAuthentication() {
		AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken("nobody", "nobody",
				ImmutableList.of(Role.ROLE_ANONYMOUS));
		SecurityContextHolder.getContext().setAuthentication(anonymousToken);
	}

}

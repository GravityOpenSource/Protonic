package ca.corefacility.bioinformatics.irida.ria.unit.web.projects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import ca.corefacility.bioinformatics.irida.model.enums.ProjectRole;
import ca.corefacility.bioinformatics.irida.model.joins.Join;
import ca.corefacility.bioinformatics.irida.model.joins.impl.ProjectUserJoin;
import ca.corefacility.bioinformatics.irida.model.joins.impl.RelatedProjectJoin;
import ca.corefacility.bioinformatics.irida.model.project.Project;
import ca.corefacility.bioinformatics.irida.model.user.User;
import ca.corefacility.bioinformatics.irida.ria.unit.TestDataFactory;
import ca.corefacility.bioinformatics.irida.ria.web.projects.ProjectControllerUtils;
import ca.corefacility.bioinformatics.irida.ria.web.projects.ProjectsController;
import ca.corefacility.bioinformatics.irida.service.ProjectService;
import ca.corefacility.bioinformatics.irida.service.TaxonomyService;
import ca.corefacility.bioinformatics.irida.service.sample.SampleService;
import ca.corefacility.bioinformatics.irida.service.user.UserService;
import ca.corefacility.bioinformatics.irida.util.TreeNode;

import com.github.dandelion.datatables.core.ajax.ColumnDef;
import com.github.dandelion.datatables.core.ajax.DatatablesCriterias;
import com.github.dandelion.datatables.core.ajax.DatatablesResponse;
import com.google.common.collect.Lists;

/**
 * Unit test for {@link }
 *
 */
public class ProjectsControllerTest {
	public static final String PROJECT_ORGANISM = "E. coli";
	private static final String USER_NAME = "testme";
	private static final User user = new User(USER_NAME, null, null, null, null, null);
	private static final String PROJECT_NAME = "test_project";
	private static final Long PROJECT_ID = 1L;
	private static final Long PROJECT_MODIFIED_DATE = 1403723706L;
	private static Project project = null;
	// Services
	private ProjectService projectService;
	private ProjectsController controller;
	private SampleService sampleService;
	private UserService userService;
	private ProjectControllerUtils projectUtils;
	private TaxonomyService taxonomyService;

	@Before
	public void setUp() {
		projectService = mock(ProjectService.class);
		sampleService = mock(SampleService.class);
		userService = mock(UserService.class);
		taxonomyService = mock(TaxonomyService.class);
		projectUtils = mock(ProjectControllerUtils.class);
		controller = new ProjectsController(projectService, sampleService, userService, projectUtils, taxonomyService);
		user.setId(1L);

		mockSidebarInfo();
	}

	@Test
	public void showAllProjects() {
		Model model = new ExtendedModelMap();
		HttpSession ses = mock(HttpSession.class);
		String page = controller.getProjectsPage(model, null,null, ses);
		assertEquals(ProjectsController.LIST_PROJECTS_PAGE, page);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetAjaxProjectList() {
		Principal principal = () -> USER_NAME;
		when(userService.getUserByUsername(USER_NAME)).thenReturn(user);

		Page<ProjectUserJoin> page = getProjectUserJoinPage(user);
		when(projectService.searchProjectUsers(any(Specification.class), any(Integer.class), any(Integer.class), any(
				Sort.Direction.class), anyVararg())).thenReturn(page);
		when(sampleService.getSamplesForProject(any(Project.class))).thenReturn(TestDataFactory.constructListJoinProjectSample());

		DatatablesCriterias criterias = mock(DatatablesCriterias.class);
		when(criterias.getColumnDefs()).thenReturn(getColumnDefs());
		when(criterias.getSortedColumnDefs()).thenReturn(getSortedColumnDefs());
		when(criterias.getLength()).thenReturn(10);

		DatatablesResponse<Map<String, Object>> result = controller.getAjaxProjectList(criterias, principal);
		testGetAnyAjaxProjectListResult(result.getData(), 10);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetAjaxAdminProjectsList() {
		when(projectService.search(any(Specification.class), any(Integer.class), any(Integer.class), any(
				Sort.Direction.class), anyVararg())).thenReturn(getProjectPage());
		when(sampleService.getSamplesForProject(any(Project.class))).thenReturn(TestDataFactory.constructListJoinProjectSample());

		DatatablesCriterias criterias = mock(DatatablesCriterias.class);
		when(criterias.getColumnDefs()).thenReturn(getColumnDefs());
		when(criterias.getSortedColumnDefs()).thenReturn(getSortedColumnDefs());
		when(criterias.getLength()).thenReturn(10);


		DatatablesResponse<Map<String, Object>> result = controller.getAjaxAdminProjectsList(criterias);

		testGetAnyAjaxProjectListResult(result.getData(), 10);
	}

	@Test
	public void testGetSpecificProjectPage() {
		Model model = new ExtendedModelMap();
		Long projectId = 1L;
		Principal principal = () -> USER_NAME;
		List<Join<Project, User>> projects = getProjectsForUser();
		when(userService.getUsersForProjectByRole(getProject(), ProjectRole.PROJECT_OWNER)).thenReturn(
				getUsersForProjectByRole());
		when(projectService.getProjectsForUser(user)).thenReturn(projects);
		when(projectService.getRelatedProjects(getProject())).thenReturn(getRelatedProjectJoin(projects));

		assertEquals("Returns the correct Project Page", ProjectsController.SPECIFIC_PROJECT_PAGE,
				controller.getProjectSpecificPage(projectId, model, principal));

	}

	@Test
	public void testGetCreateProjectPage() {
		Model model = new ExtendedModelMap();
		String page = controller.getCreateProjectPage(model);
		assertEquals("Reruns the correct New Project Page", "projects/project_new", page);
		assertTrue("Model now has and error attribute", model.containsAttribute("errors"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCreateNewProject() {
		Model model = new ExtendedModelMap();
		String projectName = "Test Project";
		Long projectId = 1002L;
		Project project = new Project(projectName);
		project.setId(projectId);
		// Test creating project
		when(projectService.create(any(Project.class))).thenReturn(project);
		when(projectService.update(eq(project.getId()), anyMap())).thenReturn(project);
		String page = controller.createNewProject(model, projectName, "", "", "");
		assertEquals("Returns the correct redirect to the collaborators page", "redirect:/projects/" + projectId
				+ "/metadata", page);
	}

	@Test
	public void testGetProjectMetadataPage() throws IOException {
		Model model = new ExtendedModelMap();
		Principal principal = () -> USER_NAME;
		String page = controller.getProjectMetadataPage(model, principal, PROJECT_ID);
		assertEquals("Returns the correct edit page.", "projects/project_metadata", page);
		assertTrue("Model should contain a project", model.containsAttribute("project"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPostProjectMetadataEditPage() throws IOException {
		Model model = new ExtendedModelMap();
		Principal principal = () -> USER_NAME;

		String newName = "My Project";
		String newOrganism = "Bad Buggy";
		String newDescritption = "Another new description.";
		String newRemoteURL = "http://ghosturl.ca";

		when(projectService.update(anyLong(), anyMap())).thenReturn(getProject());

		String page = controller.postProjectMetadataEditPage(model, principal, PROJECT_ID, newName, newOrganism,
				newDescritption, newRemoteURL);
		assertEquals("Returns the correct page.", "redirect:/projects/" + PROJECT_ID + "/metadata", page);
	}

	@Test
	public void testSearchTaxonomy() {
		String searchTerm = "bac";
		TreeNode<String> root = new TreeNode<>("Bacteria");
		TreeNode<String> child = new TreeNode<>("ChildBacteria");
		child.setParent(root);
		root.addChild(child);
		List<TreeNode<String>> resultList = new ArrayList<>();
		resultList.add(root);

		// the elements that should be at the root
		List<String> results = Lists.newArrayList(searchTerm, "Bacteria");

		when(taxonomyService.search(searchTerm)).thenReturn(resultList);
		List<Map<String, Object>> searchTaxonomy = controller.searchTaxonomy(searchTerm);

		verify(taxonomyService).search(searchTerm);

		assertFalse(searchTaxonomy.isEmpty());
		assertEquals(2, searchTaxonomy.size());

		for (Map<String, Object> element : searchTaxonomy) {
			assertTrue(element.containsKey("text"));
			assertTrue(element.containsKey("id"));
			assertTrue(results.contains(element.get("text")));
		}

	}



	/**
	 * Mocks the information found within the project sidebar.
	 */
	private void mockSidebarInfo() {
		Project project = getProject();
		Collection<Join<Project, User>> ownerList = new ArrayList<>();
		ownerList.add(new ProjectUserJoin(project, user, ProjectRole.PROJECT_OWNER));
		when(userService.getUsersForProjectByRole(any(Project.class), any(ProjectRole.class))).thenReturn(ownerList);
		when(projectService.read(PROJECT_ID)).thenReturn(project);
		when(userService.getUserByUsername(anyString())).thenReturn(user);
	}

	private List<Join<Project, User>> getProjectsForUser() {
		List<Join<Project, User>> projects = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Project p = new Project("project" + i);
			p.setId(1L + i);
			projects.add(new ProjectUserJoin(p, user, ProjectRole.PROJECT_USER));
		}
		return projects;
	}

	private List<RelatedProjectJoin> getRelatedProjectJoin(List<Join<Project, User>> projects) {
		List<RelatedProjectJoin> join = new ArrayList<>();
		Project objectProject = getProject();
		for (Join<Project, User> j : projects) {
			Project p = j.getSubject();
			join.add(new RelatedProjectJoin(objectProject, p));
		}
		// Add a couple that do not have authorization
		for (int i = 10; i < 15; i++) {
			Project p = new Project("project" + i);
			p.setId(1L + i);
			join.add(new RelatedProjectJoin(objectProject, p));
		}
		return join;
	}

	private Project getProject() {
		if (project == null) {
			project = new Project(PROJECT_NAME);
			project.setId(PROJECT_ID);
			project.setOrganism(PROJECT_ORGANISM);
			project.setModifiedDate(new Date(PROJECT_MODIFIED_DATE));
		}
		return project;
	}


	private List<Join<Project, User>> getUsersForProjectByRole() {
		List<Join<Project, User>> list = new ArrayList<>();
		list.add(new ProjectUserJoin(getProject(), user, ProjectRole.PROJECT_OWNER));
		return list;
	}

	private void testGetAnyAjaxProjectListResult(List<Map<String, Object>> result, int expectedSize) {
		assertEquals("Should be 10 items in the list", expectedSize, result.size());

		for (Map<String, Object> map : result) {
			assertTrue("Should have key 'identifier'", map.containsKey("identifier"));
			assertTrue("Should have key 'name'", map.containsKey("name"));
			assertTrue("Should have key 'organism'", map.containsKey("organism"));
			assertTrue("Should have key 'samples'", map.containsKey("samples"));
			assertTrue("Should have key 'createdDate'", map.containsKey("createdDate"));
			assertTrue("Should have key 'modifiedDate'", map.containsKey("modifiedDate"));
		}
	}

	private List<ColumnDef> getColumnDefs() {
		List<ColumnDef> list = new ArrayList<>();

		// 0. identifier
		ColumnDef def0 = new ColumnDef();
		def0.setFiltered(false);
		def0.setName("identifier");
		list.add(def0);

		// 1. name
		ColumnDef def1 = new ColumnDef();
		def1.setFiltered(false);
		def1.setName("name");
		list.add(def1);

		// 2. organism
		ColumnDef def2 = new ColumnDef();
		def2.setFiltered(false);
		def2.setName("organism");
		list.add(def2);

		return list;
	}

	private List<ColumnDef> getSortedColumnDefs() {
		List<ColumnDef> list = new ArrayList<>();

		ColumnDef def = new ColumnDef();
		def.setSortDirection(ColumnDef.SortDirection.ASC);
		def.setName("name");
		list.add(def);

		return list;
	}

	private Page<ProjectUserJoin> getProjectUserJoinPage(User user) {
		return new Page<ProjectUserJoin>() {
			@Override public int getTotalPages() {
				return 10;
			}

			@Override public long getTotalElements() {
				return 100;
			}

			@Override public int getNumber() {
				return 10;
			}

			@Override public int getSize() {
				return 10;
			}

			@Override public int getNumberOfElements() {
				return 10;
			}

			@Override public List<ProjectUserJoin> getContent() {
				return TestDataFactory.constructListJoinProjectUser(user);
			}

			@Override public boolean hasContent() {
				return true;
			}

			@Override public Sort getSort() {
				return null;
			}

			@Override public boolean isFirst() {
				return true;
			}

			@Override public boolean isLast() {
				return false;
			}

			@Override public boolean hasNext() {
				return true;
			}

			@Override public boolean hasPrevious() {
				return false;
			}

			@Override public Pageable nextPageable() {
				return null;
			}

			@Override public Pageable previousPageable() {
				return null;
			}

			@Override public Iterator<ProjectUserJoin> iterator() {
				return null;
			}
		};
	}

	public Page<Project> getProjectPage() {
		return new Page<Project>() {
			@Override public int getTotalPages() {
				return 10;
			}

			@Override public long getTotalElements() {
				return 100;
			}

			@Override public int getNumber() {
				return 10;
			}

			@Override public int getSize() {
				return 10;
			}

			@Override public int getNumberOfElements() {
				return 100;
			}

			@Override public List<Project> getContent() {
				List<Project> list = new ArrayList<>();
				for(int i = 0; i<10; i++) {
					list.add(new Project("project-" + i));
				}
				return list;
			}

			@Override public boolean hasContent() {
				return true;
			}

			@Override public Sort getSort() {
				return null;
			}

			@Override public boolean isFirst() {
				return false;
			}

			@Override public boolean isLast() {
				return false;
			}

			@Override public boolean hasNext() {
				return false;
			}

			@Override public boolean hasPrevious() {
				return false;
			}

			@Override public Pageable nextPageable() {
				return null;
			}

			@Override public Pageable previousPageable() {
				return null;
			}

			@Override public Iterator<Project> iterator() {
				return null;
			}
		};
	}
}

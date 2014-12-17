package ca.corefacility.bioinformatics.irida.model.workflow.submission.galaxy;

import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequenceFile;
import ca.corefacility.bioinformatics.irida.model.workflow.RemoteWorkflow;
import ca.corefacility.bioinformatics.irida.model.workflow.galaxy.RemoteWorkflowGalaxy;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;

/**
 * Defines an AnalysisSubmission to a Galaxy execution manager.
 * @author Aaron Petkau <aaron.petkau@phac-aspc.gc.ca>
 *
 * @param <R> The RemoteWorkflow to submit.
 */
@Entity
@Table(name = "analysis_submission_galaxy")
@Inheritance(strategy = InheritanceType.JOINED)
@Audited
@EntityListeners(AuditingEntityListener.class)
public abstract class AnalysisSubmissionGalaxy<R extends RemoteWorkflowGalaxy>
	extends AnalysisSubmission {
	
	// this relationship is not audited as the RemoteWorkflowGalaxy class is not audited
	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH, targetEntity=RemoteWorkflow.class)
	@JoinColumn(name = "remote_workflow_id")
	@NotAudited
	private R remoteWorkflow;
	
	protected AnalysisSubmissionGalaxy() {
	}
	
	/**
	 * Builds a new AnalysisSubmissionGalaxy with the given information.
	 * @param inputFiles  A set of SequenceFiles to use for the analysis.
	 * @param remoteWorkflow  A RemoteWorkflowGalaxy implementation for this analysis.
	 * @param workflowId The id of the workflow to run for this submission.
	 */
	public AnalysisSubmissionGalaxy(String name, Set<SequenceFile> inputFiles,
			R remoteWorkflow, UUID workflowId) {
		super(name, inputFiles, workflowId);
		
		this.remoteWorkflow = remoteWorkflow;
	}
	
	/**
	 * Gets a RemoteWorkflow implementing this submission.
	 * @return  A RemoteWorkflowGalaxy implementing this submission.
	 */
	public R getRemoteWorkflow() {
		return remoteWorkflow;
	}

	/**
	 * Sets the remote workflow for this analysis submission.
	 * @param remoteWorkflow  The RemoteWorkflowGalaxy for this analysis submission.
	 */
	public void setRemoteWorkflow(R remoteWorkflow) {
		this.remoteWorkflow = remoteWorkflow;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "AnalysisSubmissionGalaxy [remoteWorkflow=" + remoteWorkflow
				+ ", toString()=" + super.toString() + "]";
	}
}

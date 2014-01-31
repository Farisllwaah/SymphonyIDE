/**
 * 
 */
package eu.compassresearch.ide.faulttolerance;

import java.util.ResourceBundle;

/**
 * @author Andr&eacute; Didier (<a href=
 *         "mailto:alrd@cin.ufpe.br?Subject=Package eu.compassresearch.ide.faulttolerance, class Message"
 *         >alrd@cin.ufpe.br</a>)
 * 
 */
public enum Message {
	DIVERGENCE_FREE_JOB, SEMIFAIRNESS_JOB, FULL_FAULT_TOLERANCE_JOB, LIMITED_FAULT_TOLERANCE_JOB, STARTING_MODEL_CHECKING, CHECKING_PREREQUISITES, LIMIT_EXPRESSION, FULL_FAULT_TOLERANCE_SUCCESS, FULL_FAULT_TOLERANCE_ERROR, LIMITED_FAULT_TOLERANCE_SUCCESS, LIMITED_FAULT_TOLERANCE_ERROR, NO_PROJECT_SELECTED, MARKER_LOCATION, DIVERGENCE_FREE_SUCCESS, DIVERGENCE_FREE_ERROR, SEMIFAIR_SUCCESS, SEMIFAIR_ERROR, DIVERGENCE_FREE_SEMIFAIR_ERROR, LIMIT_EXPRESSION_DIALOG_TITLE, LIMIT_EXPRESSION_DIALOG_MESSAGE, CML_PROCESSES_TEMPLATE, BASE_CML_TEMPLATE, FOLDER_NAME, FILES_MANAGEMENT_JOB, STARTING_FAULT_TOLERANCE_FILES_MANAGEMENT, UNABLE_TO_CREATE_FAULT_TOLERANCE_PROCESSES_FILE, UNABLE_TO_CREATE_FAULT_TOLERANCE_BASE_FILE, UNABLE_TO_CREATE_FAULT_TOLERANCE_FOLDER, BASE_CML_FILE_NAME, CML_PROCESSES_FILE_NAME, LIMIT_PROCESS_NAME, NO_FAULTS_PROCESS_NAME, LAZY_PROCESS_NAME, LAZY_LIMIT_PROCESS_NAME, DIVERGENCE_FREEDOM_PROCESS_NAME, SEMIFAIRNESS_PROCESS_NAME, UNABLE_TO_CREATE_FORMULA_SCRIPT, DIVERGENCE_FREEDOM_FORMULA_SCRIPT_FILE_NAME, SEMIFAIRNESS_FORMULA_SCRIPT_FILE_NAME, FULL_FAULT_TOLERANCE_FORMULA_SCRIPT_FILE_NAME, LIMITED_FAULT_TOLERANCE_FORMULA_SCRIPT_FILE_NAME, FAULT_TOLERANCE_JOB_NAME, FAULT_TOLERANCE_VERIFICATION_TASK_MESSAGE, LAZY_DEADLOCK_CHECK_PROCESS_NAME, LAZY_LIMIT_DEADLOCK_CHECK_PROCESS_NAME, DEFINITIONS_VERIFICATION_JOB, DEFINITIONS_VERIFICATION_TASK_NAME, EXISTING_NEEDED_CHANNELS, EXISTING_NEEDED_CHANSETS, EXISTING_NEEDED_PROCESSES, CHECK_NAMES_TASK, CHANNELS_NOT_FOUND, CHANSETS_NOT_FOUND, PROCESSES_NOT_FOUND, MISSING_DEFINITIONS, UNABLE_TO_FIND_PROJECT_DEFINITIONS, VALUES_NOT_FOUND, EXISTING_NEEDED_VALUES, EXISTING_NEEDED_NAMESETS, NAMESETS_NOT_FOUND, CHANSET_F_TEMPLATE, CHANSET_E_TEMPLATE, CHANSET_H_TEMPLATE, CHANSET_E_NAME, CHANSET_F_NAME, CHANSET_H_NAME, CHANSET_H_RELATED, PROCESS_RUN_E_NAME, PROCESS_RUN_E_TEMPLATE, PROCESS_CHAOS_E_NAME, PROCESS_CHAOS_E_TEMPLATE, LIMIT_PROCESS_TEMPLATE, DIVERGENCE_FREEDOM_PROCESS_TEMPLATE, SEMIFAIRNESS_PROCESS_TEMPLATE, LAZY_DEADLOCK_CHECK_PROCESS_TEMPLATE, LAZY_LIMIT_DEADLOCK_CHECK_PROCESS_TEMPLATE, NO_FAULTS_PROCESS_TEMPLATE, LAZY_PROCESS_TEMPLATE, LAZY_LIMIT_PROCESS_TEMPLATE, EXCEPTION_OCCURRED, CANCELLED_BY_USER;

	public String format(Object... params) {
		return String.format(
				ResourceBundle.getBundle("Message").getString(this.name()),
				params);
	}
}

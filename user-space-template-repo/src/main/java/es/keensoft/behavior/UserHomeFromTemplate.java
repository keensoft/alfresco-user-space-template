package es.keensoft.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.rule.RuleModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.apache.log4j.Logger;

public class UserHomeFromTemplate implements NodeServicePolicies.OnCreateNodePolicy {

	private PolicyComponent policyComponent;
	private ServiceRegistry serviceRegistry;
	private String userHomeTemplatePath;

	private static final String KEY_RELATED_USERS = UserHomeFromTemplate.class.getName() + ".createdUsers";
	private TransactionListener transactionListener;

	private Logger logger = Logger.getLogger(UserHomeFromTemplate.class);

	public void init() {
		Behaviour onCreateNode = new JavaBehaviour(this, "onCreateNode", NotificationFrequency.TRANSACTION_COMMIT);
		policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, ContentModel.TYPE_PERSON, onCreateNode);
		transactionListener = new UserCreatedTransactionListener();
	}

	public void onCreateNode(ChildAssociationRef childAssocRef) {

		AlfrescoTransactionSupport.bindListener(transactionListener);

		NodeRef userNodeRef = childAssocRef.getChildRef();

		List<NodeRef> currentRelatedUsers = AlfrescoTransactionSupport.getResource(KEY_RELATED_USERS);
		if (currentRelatedUsers == null) {
			currentRelatedUsers = new ArrayList<NodeRef>();
		}
		currentRelatedUsers.add(userNodeRef);
		AlfrescoTransactionSupport.bindResource(KEY_RELATED_USERS, currentRelatedUsers);

	}

	private class UserCreatedTransactionListener extends TransactionListenerAdapter implements TransactionListener {

		@Override
		public void afterCommit() {

			@SuppressWarnings("unchecked")
			List<NodeRef> userCreated = (List<NodeRef>) AlfrescoTransactionSupport.getResource(KEY_RELATED_USERS);

			if (userCreated != null) {

				ExecutorService executor = Executors.newFixedThreadPool(5);

				for (NodeRef userNodeRef : userCreated) {

					try {

						Runnable runnable = new UserSpaceTemplateCreation(userNodeRef);
						executor.execute(runnable);

					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				}
			}
		}

		@Override
		public void flush() {
		}

	}

	private class UserSpaceTemplateCreation implements Runnable {

		private NodeRef userRef;

		private UserSpaceTemplateCreation(NodeRef userRef) {
			this.userRef = userRef;
		}

		@Override
		public void run() {
			
			AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

				public Void doWork() throws Exception {

					RetryingTransactionCallback<Void> callback = new RetryingTransactionCallback<Void>() {

						@Override
						public Void execute() throws Throwable {

							NodeService nodeService = serviceRegistry.getNodeService();
							SearchService searchService = serviceRegistry.getSearchService();
							FileFolderService fileFolderService = serviceRegistry.getFileFolderService();
							CopyService copyService = serviceRegistry.getCopyService();

							try {

								// User home folder
								NodeRef homeFolder = (NodeRef) nodeService.getProperty(userRef, ContentModel.PROP_HOMEFOLDER);

								// Template folder
								ResultSet rs = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE,
										"+PATH:\"/" + userHomeTemplatePath + "\"");
								if (rs.length() == 0) {
									throw new RuntimeException("Please verify that user home template folder exists at " + userHomeTemplatePath);
								}
								NodeRef userTemplate = rs.getNodeRef(0);

								// Contents
								List<ChildAssociationRef> children = nodeService.getChildAssocs(userTemplate);
								for (ChildAssociationRef childRef : children) {
									NodeRef contentNode = childRef.getChildRef();
									logger.info("Copying " + nodeService.getProperty(contentNode, ContentModel.PROP_NAME) + " to user home folder...");
									fileFolderService.copy(contentNode, homeFolder, null);
								}

								// Rules
								List<ChildAssociationRef> assocs = nodeService.getChildAssocs(homeFolder, RuleModel.ASSOC_RULE_FOLDER,
										RegexQNamePattern.MATCH_ALL);
								for (ChildAssociationRef ruleFolderAssoc : assocs) {
									NodeRef ruleFolder = ruleFolderAssoc.getChildRef();
									logger.info("Copying " + nodeService.getProperty(ruleFolder, ContentModel.PROP_NAME) + " to user home folder...");
									copyService.copy(ruleFolder, homeFolder, RuleModel.ASSOC_RULE_FOLDER, RuleModel.ASSOC_RULE_FOLDER, true);
								}

							} catch (Exception e) {
								throw new RuntimeException(e);
							}

							return null;
						}
					};

					try {
						RetryingTransactionHelper txnHelper = serviceRegistry.getTransactionService().getRetryingTransactionHelper();
						txnHelper.doInTransaction(callback, false, true);
					} catch (Throwable e) {
						logger.error("User home contents for " + userRef + " has not been copied!", e);
					}

					return null;

				}
			});
		}
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setUserHomeTemplatePath(String userHomeTemplatePath) {
		this.userHomeTemplatePath = userHomeTemplatePath;
	}

}

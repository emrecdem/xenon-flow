package nl.esciencecenter.computeservice.rest.service.tasks;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.commonwl.cwl.CwlException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.JsonMappingException;

import nl.esciencecenter.computeservice.config.AdaptorConfig;
import nl.esciencecenter.computeservice.config.ComputeServiceConfig;
import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobState;
import nl.esciencecenter.computeservice.rest.model.StatePreconditionException;
import nl.esciencecenter.computeservice.rest.model.WorkflowBinding;
import nl.esciencecenter.computeservice.rest.service.staging.FileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifestFactory;
import nl.esciencecenter.computeservice.rest.service.staging.StagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.StringToFileStagingObject;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {nl.esciencecenter.computeservice.rest.Application.class})
public class CwlStageInTest {
	@Value("${xenon.log.basepath}")
	private Path logBasePath;
	
	@Value("${xenon.config}")
	private String xenonConfigFile;
	
	private FileSystem sourceFileSystem;
	
	private FileSystem getSourceFileSystem() throws XenonException, IOException {
		if (sourceFileSystem == null || !sourceFileSystem.isOpen()) {
			
			ComputeServiceConfig config = ComputeServiceConfig.loadFromFile(new File(xenonConfigFile));
			
			// Initialize local filesystem
			AdaptorConfig sourceConfig = config.getSourceFilesystemConfig();
			sourceFileSystem = FileSystem.create(sourceConfig.getAdaptor(), sourceConfig.getLocation(),
					sourceConfig.getCredential(), sourceConfig.getProperties());
		}
		return sourceFileSystem;
	}

	@Test
	public void createStagingManifestTest() throws JsonMappingException, IOException, StatePreconditionException, CwlException, XenonException {
		String uuid = UUID.randomUUID().toString();

		Logger jobLogger = LoggerFactory.getLogger("jobs." + uuid);

		Job job = new Job();
		job.setId(uuid);
		job.setInput(WorkflowBinding.fromFile(new File("src/test/resources/cwl/echo-file.json")));
		job.setName("createStagingManifestTest");
		job.setInternalState(JobState.SUBMITTED);
		job.setWorkflow("src/test/resources/cwl/echo-file.cwl");
		
		StagingManifest manifest = StagingManifestFactory.createStagingManifest(job, this.getSourceFileSystem(), jobLogger);
		
		List<String> paths = new ArrayList<String>();
		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			} else if (stageObject instanceof StringToFileStagingObject) {
				StringToFileStagingObject object = (StringToFileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			}
		}
		
		List<String> expected = Arrays.asList("echo-file.cwl", "echo-file.json", "job-order.json");
		assertEquals("Expecting arrays to be equal", expected, paths);
	}
	
	@Test
	public void createStagingManifestMultiFileTest() throws JsonMappingException, IOException, StatePreconditionException, CwlException, XenonException {
		String uuid = UUID.randomUUID().toString();

		Logger jobLogger = LoggerFactory.getLogger("jobs." + uuid);

		Job job = new Job();
		job.setId(uuid);
		job.setWorkflow("src/test/resources/cwl/count-lines-remote.cwl");
		job.setInput(WorkflowBinding.fromFile(new File("src/test/resources/cwl/count-lines-job.json")));
		job.setName("createStagingManifestTest");
		job.setInternalState(JobState.SUBMITTED);
		
		StagingManifest manifest = StagingManifestFactory.createStagingManifest(job, this.getSourceFileSystem(), jobLogger);
		
		List<String> paths = new ArrayList<String>();
		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			} else if (stageObject instanceof StringToFileStagingObject) {
				StringToFileStagingObject object = (StringToFileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			}
		}
		
		List<String> expected = Arrays.asList("count-lines-remote.cwl", "parseInt-tool.cwl", "ipsum.txt", "job-order.json");
		assertEquals("Expecting arrays to be equal", expected, paths);
	}
}
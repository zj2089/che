/*******************************************************************************
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.api.project;

import com.google.gwt.http.client.RequestBuilder;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.NewProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.machine.DevMachine;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.rest.AsyncRequest;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.StringUnmarshaller;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;
import org.eclipse.che.ide.ui.loaders.request.MessageLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.gwt.http.client.RequestBuilder.PUT;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertEquals;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.google.gwt.http.client.RequestBuilder.DELETE;

/**
 * Unit test for {@link ProjectServiceClientImpl}.
 *
 * @author Vlad Zhukovskyi
 * @author Oleksander Andriienko
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectServiceClientImplTest {

    private static final String TEXT = "to be or not to be.";

    @Mock
    private LoaderFactory          loaderFactory;
    @Mock
    private AsyncRequestFactory    requestFactory;
    @Mock
    private DtoFactory             dtoFactory;
    @Mock
    private DtoUnmarshallerFactory unmarshaller;
    @Mock
    private AppContext             appContext;
    @Mock
    private AsyncRequest           asyncRequest;

    @Mock
    private Unmarshallable<ItemReference> unmarshallableItemRef;
    @Mock
    private Unmarshallable<List<ProjectConfigDto>>  unmarshallablePrjConf;

    @Mock
    private Promise<ItemReference> itemRefPromise;
    @Mock
    private MessageLoader messageLoader;

    @Mock
    private NewProjectConfigDto prjConfig1;
    @Mock
    private NewProjectConfigDto prjConfig2;

    @Captor
    private ArgumentCaptor<String> argumentCaptor;
    @Captor
    private ArgumentCaptor<List<NewProjectConfigDto>> prjsArgCaptor;

    private ProjectServiceClientImpl projectServiceClient;
    private Path resourcePath = Path.valueOf("TestPrj/http%253A%252F%252Fwinery.opentosca.org%252Fte st ");

    @Before
    public void setUp() throws Exception {
        projectServiceClient = new ProjectServiceClientImpl(loaderFactory,
                                                            requestFactory,
                                                            dtoFactory,
                                                            unmarshaller,
                                                            appContext);
        DevMachine devMachine = mock(DevMachine.class);
        when(devMachine.getWsAgentBaseUrl()).thenReturn("http://127.0.0.3/api");

        when(appContext.getDevMachine()).thenReturn(devMachine);
        when(loaderFactory.newLoader(any())).thenReturn(messageLoader);
        when(asyncRequest.loader(messageLoader)).thenReturn(asyncRequest);
        when(asyncRequest.data(any())).thenReturn(asyncRequest);
        when(asyncRequest.send(unmarshallableItemRef)).thenReturn(itemRefPromise);
        when(asyncRequest.header(any(), any())).thenReturn(asyncRequest);
        when(unmarshaller.newUnmarshaller(ItemReference.class)).thenReturn(unmarshallableItemRef);
        when(unmarshaller.newListUnmarshaller(ProjectConfigDto.class)).thenReturn(unmarshallablePrjConf);
    }

    @Test
    public void testShouldNotSetupLoaderForTheGetTreeMethod() throws Exception {
        AsyncRequest asyncRequest = mock(AsyncRequest.class);

        when(requestFactory.createGetRequest(anyString())).thenReturn(asyncRequest);
        when(asyncRequest.header(anyString(), anyString())).thenReturn(asyncRequest);

        projectServiceClient.getTree(Path.EMPTY, 1, true);

        verify(asyncRequest, never()).loader(any(AsyncRequestLoader.class)); //see CHE-3467
    }

    @Test
    public void shouldSearchResourceReferencesByEncodedUrl() {
        Map<String, String> options = singletonMap(TEXT, TEXT);

        QueryExpression expression = new QueryExpression();
        expression.setName(TEXT);
        expression.setPath(resourcePath.toString());
        expression.setMaxItems(100);
        expression.setSkipCount(10);


//        projectServiceClient.search(prjConfig1, options);
    }

    @Test
    public void shouldCreateOneProjectByBatch() {
        List<NewProjectConfigDto> configs = singletonList(prjConfig1);
        when(requestFactory.createPostRequest(anyString(), any(MimeType.class))).thenReturn(asyncRequest);

        projectServiceClient.createBatchProjects(configs);

        verify(requestFactory).createPostRequest(argumentCaptor.capture(), prjsArgCaptor.capture());
        verify(asyncRequest).header(ACCEPT, MimeType.APPLICATION_JSON);
        verify(loaderFactory).newLoader("Creating project...");
        verify(asyncRequest).loader(messageLoader);
        verify(asyncRequest).send(unmarshallablePrjConf);
        verify(unmarshaller).newListUnmarshaller(ProjectConfigDto.class);

        assertEquals("http://127.0.0.3/api/project/batch", argumentCaptor.getValue());
        assertEquals(1, prjsArgCaptor.getValue().size());
    }

    @Test
    public void shouldCreateProjectsByBatch() {
        List<NewProjectConfigDto> configs = Arrays.asList(prjConfig1, prjConfig2);
        when(requestFactory.createPostRequest(anyString(), any(MimeType.class))).thenReturn(asyncRequest);

        projectServiceClient.createBatchProjects(configs);

        verify(requestFactory).createPostRequest(argumentCaptor.capture(), prjsArgCaptor.capture());
        verify(asyncRequest).header(ACCEPT, MimeType.APPLICATION_JSON);
        verify(loaderFactory).newLoader("Creating the batch of projects...");
        verify(asyncRequest).loader(messageLoader);
        verify(asyncRequest).send(unmarshallablePrjConf);
        verify(unmarshaller).newListUnmarshaller(ProjectConfigDto.class);

        assertEquals("http://127.0.0.3/api/project/batch", argumentCaptor.getValue());
        assertEquals(2, prjsArgCaptor.getValue().size());
    }

    @Test
    public void shouldCreateFileByEncodedUrl() {
        when(requestFactory.createPostRequest(anyString(), any())).thenReturn(asyncRequest);

        projectServiceClient.createFile(resourcePath, TEXT);

        verify(requestFactory).createPostRequest(argumentCaptor.capture(), any());
        verify(asyncRequest).data(TEXT);
        verify(loaderFactory).newLoader("Creating file...");
        verify(asyncRequest).loader(messageLoader);
        verify(asyncRequest).send(unmarshallableItemRef);

        assertEquals("http://127.0.0.3/api/project/file/TestPrj?name=http%25253A%25252F%25252Fwinery.opentosca.org%25252Fte%20st%20",
                     argumentCaptor.getValue());
    }

    @Test
    public void shouldGetFileContentByEncodedUrl() {
        when(requestFactory.createGetRequest(anyString())).thenReturn(asyncRequest);

        projectServiceClient.getFileContent(resourcePath);

        verify(requestFactory).createGetRequest(argumentCaptor.capture());
        verify(loaderFactory).newLoader("Loading file content...");
        verify(asyncRequest).loader(messageLoader);
        verify(asyncRequest).send(any(StringUnmarshaller.class));

        assertEquals("http://127.0.0.3/api/project/file/TestPrj/http%25253A%25252F%25252Fwinery.opentosca.org%25252Fte%20st%20",
                     argumentCaptor.getValue());
    }

    @Test
    public void shouldSetFileContentByEncodedUrl() {
        when(requestFactory.createRequest(any(RequestBuilder.Method.class), anyString(), any(), anyBoolean())).thenReturn(asyncRequest);

        projectServiceClient.setFileContent(resourcePath, TEXT);

        verify(requestFactory).createRequest(eq(PUT), argumentCaptor.capture(), any(), eq(false));
        verify(asyncRequest).data(TEXT);
        verify(loaderFactory).newLoader("Updating file...");
        verify(asyncRequest).loader(messageLoader);
        verify(asyncRequest).send();

        assertEquals("http://127.0.0.3/api/project/file/TestPrj/http%25253A%25252F%25252Fwinery.opentosca.org%25252Fte%20st%20",
                     argumentCaptor.getValue());
    }

    @Test
    public void shouldCreateFolderByEncodedUrl() {
        when(requestFactory.createPostRequest(Matchers.anyString(), any())).thenReturn(asyncRequest);

        projectServiceClient.createFolder(resourcePath);

        verify(requestFactory).createPostRequest(argumentCaptor.capture(), any());
        verify(loaderFactory).newLoader("Creating folder...");
        verify(asyncRequest).loader(messageLoader);
        verify(unmarshaller).newUnmarshaller(ItemReference.class);
        verify(asyncRequest).send(unmarshallableItemRef);

        assertEquals("http://127.0.0.3/api/project/folder/TestPrj/http%25253A%25252F%25252Fwinery.opentosca.org%25252Fte%20st%20",
                     argumentCaptor.getValue());
    }

    @Test
    public void shouldDeleteFolderByEncodedUrl() {
        when(requestFactory.createRequest(any(RequestBuilder.Method.class), Matchers.anyString(), any(), anyBoolean())).thenReturn(asyncRequest);

        projectServiceClient.deleteItem(resourcePath);

        verify(requestFactory).createRequest(eq(DELETE), argumentCaptor.capture(), any(), eq(false));
        verify(loaderFactory).newLoader("Deleting resource...");
        verify(asyncRequest).loader(messageLoader);
        verify(asyncRequest).send();

        assertEquals("http://127.0.0.3/api/project/TestPrj/http%25253A%25252F%25252Fwinery.opentosca.org%25252Fte%20st%20",
                     argumentCaptor.getValue());
    }
}
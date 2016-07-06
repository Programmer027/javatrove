/*
 * Copyright 2016 Andres Almiray
 *
 * This file is part of Java Trove Examples
 *
 * Java Trove Examples is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Trove Examples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Trove Examples. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kordamp.javatrove.example01;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import javafx.scene.control.Button;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kordamp.javatrove.example01.model.Repository;
import org.kordamp.javatrove.example01.service.GithubAPI;
import org.kordamp.javatrove.example01.util.ApplicationEventHandler;
import org.kordamp.javatrove.example01.view.AppView;
import org.testfx.framework.junit.ApplicationRule;
import org.testfx.service.support.WaitUntilSupport;

import javax.inject.Inject;
import java.util.Collection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.inject.name.Names.named;
import static org.kordamp.javatrove.example01.TestHelper.createSampleRepositories;
import static org.kordamp.javatrove.example01.TestHelper.repositoriesAsJSON;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.testfx.matcher.control.LabeledMatchers.hasText;
import static org.testfx.matcher.control.ListViewMatchers.hasItems;

/**
 * @author Andres Almiray
 */
@RunWith(JukitoRunner.class)
@UseModules({FunctionalTest.AppTestModule.class})
public class FunctionalTest {
    private static final String ORGANIZATION = "foo";

    @Inject private ObjectMapper objectMapper;
    @Inject private AppView view;
    @Inject private ApplicationEventHandler eventHandler;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Rule
    public ApplicationRule textfx = new ApplicationRule(stage -> {
        stage.setScene(view.createScene());
        stage.sizeToScene();
        stage.setResizable(false);
        stage.show();
    });

    @Test
    public void happyPath() throws Exception {
        // given:
        Collection<Repository> repositories = createSampleRepositories();
        stubFor(get(urlEqualTo("/orgs/" + ORGANIZATION + "/repos"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/json")
                .withBody(repositoriesAsJSON(repositories, objectMapper))));
        textfx.clickOn("#organization")
            .eraseText(ORGANIZATION.length())
            .write(ORGANIZATION);

        // when:
        textfx.clickOn("#loadButton");

        // wait
        Button loadButton = textfx.lookup("#loadButton").query();
        new WaitUntilSupport().waitUntil(loadButton, isEnabled(), 2);

        // then:
        verifyThat("#total", hasText("3"));
        verifyThat("#repositories", hasItems(3));
    }

    @Test
    public void failurePath() {
        // given:
        stubFor(get(urlEqualTo("/orgs/" + ORGANIZATION + "/repos"))
            .willReturn(aResponse()
                .withStatus(500)
                .withStatusMessage("Internal Error")));
        textfx.clickOn("#organization")
            .eraseText(ORGANIZATION.length())
            .write(ORGANIZATION);

        // when:
        textfx.clickOn("#loadButton");

        // wait
        Button loadButton = textfx.lookup("#loadButton").query();
        new WaitUntilSupport().waitUntil(loadButton, isEnabled(), 5);
    }

    public static class AppTestModule extends AppModule {
        @Override
        protected void bindGithubApiUrl() {
            bindConstant()
                .annotatedWith(named(GithubAPI.GITHUB_API_URL_KEY))
                .to("http://localhost:8080");
        }
    }
}

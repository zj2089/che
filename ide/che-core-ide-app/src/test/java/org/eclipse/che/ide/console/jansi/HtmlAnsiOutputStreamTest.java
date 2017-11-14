/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.console.jansi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.Test;

public class HtmlAnsiOutputStreamTest {
  @Test
  public void testHtml() throws IOException {
    String input = "[ERROR] Failed to execute goal \u001B[32mnet.ltgt.gwt.maven:gwt-maven-plugin:1.0-rc-8:compile\u001B[m \u001B[1m(default)\u001B[m on project \u001B[36massembly-ide-war\u001B[m: \u001B[1;31mGWT exited with status 137\u001B[m -> \u001B[1m[Help 1]\u001B[m";
    //String input = "[INFO] \u001B[1m--- \u001B[0;32mmaven-enforcer-plugin:3.0.0-M1:enforce\u001B[m \u001B[1m(dependency)\u001B[m @ \u001B[36mplugin-menu-ide\u001B[0;1m ---\u001B[m";
    //String input = "[INFO] \u001B[1m------------------------------------------------------------------------\u001B[m";
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    // BufferedOutputStream bos = new BufferedOutputStream(os);
    HtmlAnsiOutputStream html = new HtmlAnsiOutputStream(os);
    html.write(input.getBytes());
    html.close();
    String coloredText = new String(os.toByteArray());
    System.out.println(coloredText);
  }
}

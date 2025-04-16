/*
 * Copyright (C) 2025 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

public class ContextParameterTest {

  @Test
  public void buildContextParameter() {
    ContextParameter parameter = ContextParameter.builder()
      .setName("logger")
      .setType(ClassNames.get(String.class))
      .build();

    assertThat(parameter.getName()).isEqualTo("logger");
    assertThat(parameter.getType()).isEqualTo(ClassNames.get(String.class));
  }

  @Test
  public void buildContextParameterWithDifferentTypes() {
    ContextParameter stringParam = ContextParameter.builder()
      .setName("stringParam")
      .setType(ClassNames.get(String.class))
      .build();

    ContextParameter intParam = ContextParameter.builder()
      .setName("intParam")
      .setType(ClassNames.get(Integer.class))
      .build();

    assertThat(stringParam.getType()).isEqualTo(ClassNames.get(String.class));
    assertThat(intParam.getType()).isEqualTo(ClassNames.get(Integer.class));
  }

  @Test
  public void contextParameterToString() {
    ContextParameter parameter = ContextParameter.builder()
      .setName("logger")
      .setType(ClassNames.get(String.class))
      .build();

    assertThat(parameter.toString()).isEqualTo("logger: java.lang.String");
  }

  @Test
  public void contextParameterEquality() {
    ContextParameter parameter1 = ContextParameter.builder()
      .setName("logger")
      .setType(ClassNames.get(String.class))
      .build();

    ContextParameter parameter2 = ContextParameter.builder()
      .setName("logger")
      .setType(ClassNames.get(String.class))
      .build();

    ContextParameter parameter3 = ContextParameter.builder()
      .setName("different")
      .setType(ClassNames.get(String.class))
      .build();

    assertThat(parameter1).isEqualTo(parameter2);
    assertThat(parameter1).isNotEqualTo(parameter3);
  }

  @Test
  public void buildWithNoNameShouldThrow() {
    Throwable throwable = assertThrows(IllegalArgumentException.class, () ->
      ContextParameter.builder()
        .setType(ClassNames.get(String.class))
        .build());
    assertThat(throwable).hasMessageThat().isEqualTo("name was not set");
  }

  @Test
  public void buildWithNoTypeShouldThrow() {
    Throwable throwable = assertThrows(IllegalArgumentException.class, () ->
      ContextParameter.builder()
        .setName("logger")
        .build());
    assertThat(throwable).hasMessageThat().isEqualTo("type was not set");
  }

  @Test
  public void buildWithNoNameAndNoTypeShouldThrow() {
    Throwable throwable = assertThrows(IllegalArgumentException.class, () ->
      ContextParameter.builder()
        .build());
    assertThat(throwable).hasMessageThat().isEqualTo("name was not set, type was not set");
  }

  @Test
  public void buildWithBlankNameAndNoTypeShouldThrow() {
    Throwable throwable = assertThrows(IllegalArgumentException.class, () ->
      ContextParameter.builder()
        .setName(" ")
        .build());
    assertThat(throwable).hasMessageThat().isEqualTo("name is blank, type was not set");
  }
}

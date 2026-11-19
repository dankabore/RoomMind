package com.roommind.dto;

import com.roommind.enums.Language;

import lombok.Builder;
import lombok.Getter;

/**
 * One message translated for the person who asked. The original is not
 * repeated: the screen already has it, and keeps showing it when the
 * translation is toggled off.
 *
 * The language is included because the reader may change it on their profile
 * while the translation is on screen; this says which one it is in.
 */
@Getter
@Builder
public class TranslationResponse {

	private final Long messageId;

	private final Language language;

	private final String translation;
}

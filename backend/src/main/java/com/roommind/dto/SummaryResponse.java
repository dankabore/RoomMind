package com.roommind.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * What the summarize button gets back. A single field for now, but an object
 * rather than a bare string so that what is added later (such as which message
 * the summary reaches up to) does not change the shape of the reply.
 */
@Getter
@Builder
public class SummaryResponse {

	private final String summary;
}

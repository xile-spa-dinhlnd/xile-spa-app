package com.xilespa.module.catalog.dto.response;

import java.util.List;

public record ComboQuoteResponse(List<ComboItemResponse> items, long retailTotal, Long savings) {}

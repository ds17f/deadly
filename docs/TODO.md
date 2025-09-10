# TODO List

This document tracks known issues, missing features, and improvements needed in the KMM migration.

## Search Feature Migration

### Resources & Assets
- [ ] Add background alt logo image for DecadeCard components (referenced in V2 as `com.deadly.v2.core.design.R.drawable.alt_logo`)
- [ ] Add Material Symbols font to `androidMain/res/font/material_symbols_outlined.ttf` (from https://github.com/google/material-design-icons/tree/master/variablefont)
- [ ] Set up resource handling for cross-platform image assets

### Theme & Design System
- [ ] Set up centralized theme colors that work across Android and iOS platforms
- [ ] Configure MaterialTheme.colorScheme.primary to be accessible from iOS UIKit interop

### Navigation Integration
- [ ] Implement navigation callbacks for decade browsing
- [ ] Implement navigation callbacks for discover items  
- [ ] Implement navigation callbacks for browse all items
- [ ] Connect to search results screen navigation

### Service Implementation
- [ ] Replace `SearchServiceStub` with real `SearchServiceImpl` when ready
- [ ] Implement actual search backend integration
- [ ] Add proper error handling and loading states

## Phase 3 & 4 (Future)
- [ ] SQLDelight data layer integration
- [ ] Data import service from Archive.org
- [ ] Real search backend implementation
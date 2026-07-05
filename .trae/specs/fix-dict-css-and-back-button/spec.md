# Fix Dictionary CSS and Back Button Spec

## Why
1. Oxford dictionary CSS not loading correctly - CSS injection order issue in HtmlContentBuilder
2. Back button not working on Windows desktop - JCEF WebView intercepting click events

## What Changes
- Fix CSS injection order in HtmlContentBuilder for DefaultRenderer
- Fix back button event handling in desktop WordDetailScreen

## Impact
- Affected specs: WebView rendering, Navigation
- Affected code: HtmlContentBuilder.kt, WordDetailScreen.kt

## MODIFIED Requirements
### Requirement: Dictionary CSS Loading
The system SHALL correctly load dictionary CSS while maintaining transparent background

### Requirement: Back Button Navigation
The system SHALL allow back button clicks on Windows desktop

## ADDED Requirements
### Requirement: CSS Loading Priority
The system SHALL load dictionary CSS after base styles to allow proper override

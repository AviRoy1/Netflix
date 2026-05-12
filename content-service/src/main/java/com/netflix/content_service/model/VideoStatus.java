package com.netflix.content_service.model;

/**
 * Flow
 * PENDING -> UPLOADED -> ENCODING -> ENCODED -> READY
 *                                 -> FAILED
 */

public enum VideoStatus {
    PENDING,
    UPLOADED,
    ENCODING,
    ENCODED,
    READY,
    FAILED
}

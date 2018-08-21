package me.nocturnl.inksnap

class SubmissionException(val reasonTitle: String, val detailedReason: String) : Exception(detailedReason)
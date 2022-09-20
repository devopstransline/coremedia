# Known Issues (and Open Questions)

## DownloadFromTranslineAction: Failure Message Flood

If a download fails (in the following example, invalid/unexpected XLIFF), `DownloadFromTranslineAction`
files a warning. While this is ok, for the first time, the warning is actually
filed on every pull from TLC server which may end in a flood of messages.

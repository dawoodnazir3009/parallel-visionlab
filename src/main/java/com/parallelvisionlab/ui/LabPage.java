package com.parallelvisionlab.ui;

enum LabPage {
    OVERVIEW("Overview", "Your parallel image processing command center", "Workspace"),
    WORKSPACE("Image Workspace", "Load images and configure your next processing experiment", "Workspace"),
    BENCHMARK("Benchmark Lab", "Compare execution time, speedup and throughput", "Workspace"),
    MONITOR("Live Hardware Monitor", "Runtime resources and backend availability", "Performance"),
    HISTORY("Experiment History", "Review experiment records and export sample data", "Performance"),
    SETTINGS("Settings", "Runtime information and benchmark preferences", "System");

    final String title;
    final String subtitle;
    final String group;

    LabPage(String title, String subtitle, String group) {
        this.title = title;
        this.subtitle = subtitle;
        this.group = group;
    }
}

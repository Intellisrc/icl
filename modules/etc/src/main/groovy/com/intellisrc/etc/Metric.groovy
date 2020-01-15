package com.intellisrc.etc

import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Keep track of some metrics
 * The main function is that whenever a value changes, it will trigger `onChange` event,
 * which is useful to prevent flooding some output when the value barely changes.
 * @since 2019/09/11.
 */
@CompileStatic
class Metric {
    static public ConcurrentLinkedQueue<Metric> metrics = new ConcurrentLinkedQueue<>()
    
    interface MetricChanged {
        void call(double value)
    }
    
    interface MetricFormatter {
        String call(double value)
    }
    
    final String type
    private double last = 0
    MetricChanged callback
    MetricFormatter formatter
    
    static Metric get(String type) {
        Metric metric = metrics.find { it.type == type }
        if(!metric) {
            metric = new Metric(type)
            metrics << metric
        }
        return metric
    }
    static void add(String type, double value) {
        get(type).update(value)
    }
    
    private Metric(String type) {
        this.type = type
    }
    
    void update(double value) {
        if(last != value) {
            last = value
            if(callback) {
                callback.call(value)
            }
        }
    }
    
    Metric onChange(MetricChanged callback) {
        this.callback = callback
        return this
    }
    
    Metric setFormatter(MetricFormatter formatter) {
        this.formatter = formatter
        return this
    }
    
    void reset() {
        last = 0
    }
    
    double getValue() {
        return last ?: 0
    }
    /**
     * Return text for log
     * @return
     */
    String getText() {
        return formatter ? formatter.call(last) : type.toString() + ":" + String.format("%.2f", last)
    }
}

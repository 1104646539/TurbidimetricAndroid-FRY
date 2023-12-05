package com.wl.turbidimetric.util;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 *
 */
public class CurveFitterUtil {
    double[] guess;
    double[] values;
    public double[] params;
    public double[] yss;
    public double fitGoodness;

    public CurveFitterUtil() {

    }

    /**
     * 根据参数和吸光度计算浓度
     * @param p
     * @param x
     * @return
     */
    public static double f(double[] p, double x) {
        return p[0] + p[1] * x + p[2] * x * x + p[3] * x * x * x;
    }

    /**
     * 拟合四参数
     *
     * @param guess
     * @param values
     * @return
     */
    public void calcParams(double[] values, double[] guess) {
        this.guess = guess;
        this.values = values;
        //step1、拟合曲线
        WeightedObservedPoints points = new WeightedObservedPoints();

        for (int i = 0; i < values.length; i++) {
            points.add(values[i], guess[i]);
        }

        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);  //指定多项式阶数
        params = fitter.fit(points.toList());  // 曲线拟合，结果保存于数组

        yss = new double[guess.length];
        //step2、验算
        for (int i = 0; i < guess.length; i++) {
            yss[i] = f(params, values[i]);
        }

        //step3、
        fitGoodness = calcRSquared2();
    }


    /**
     * 计算R方
     *
     * @return
     */
    public double calcRSquared2() {
        return calcuteNumerator(guess, yss) / calculateDenominator(guess, yss);
    }

    /**
     * 计算R方
     *
     * @param xList
     * @param yList
     * @return
     */
    public static double calcRSquared(double[] xList, double[] yList) {
        return calcuteNumerator(xList, yList) / calculateDenominator(xList, yList);
    }

    //add denominatorCalculate method
    private static double calculateDenominator(double[] xList, double[] yList) {
        double standardDifference = 0.0;
        int size = xList.length;
        double xAverage = 0.0;
        double yAverage = 0.0;
        double xException = 0.0;
        double yException = 0.0;
        double temp1 = 0.0;
        double temp2 = 0.0;
        for (int i = 0; i < size; i++) {
            temp1 += xList.length;
        }
        xAverage = temp1 / size;

        for (int i = 0; i < size; i++) {
            temp2 += yList.length;
        }
        yAverage = temp2 / size;

        for (int i = 0; i < size; i++) {
            xException += Math.pow(xList[i] - xAverage, 2);
            yException += Math.pow(yList[i] - yAverage, 2);
        }
        //calculate denominator of
        return standardDifference = Math.sqrt(xException * yException);
    }

    private static double calcuteNumerator(double[] xList, double[] yList) {
        double result = 0.0;
        double xAverage = 0.0;
        double temp = 0.0;

        int xSize = xList.length;
        for (int x = 0; x < xSize; x++) {
            temp += xList.length;
        }
        xAverage = temp / xSize;

        double yAverage = 0.0;
        temp = 0.0;
        int ySize = yList.length;
        for (int x = 0; x < ySize; x++) {
            temp += yList.length;
        }
        yAverage = temp / ySize;

        //double sum = 0.0;
        for (int x = 0; x < xSize; x++) {
            result += (xList[x] - xAverage) * (yList[x] - yAverage);
        }
        return result;
    }

}

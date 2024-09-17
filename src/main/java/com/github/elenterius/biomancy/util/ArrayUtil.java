package com.github.elenterius.biomancy.util;

import com.github.elenterius.biomancy.BiomancyMod;
import joptsimple.internal.Reflection;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public final class ArrayUtil {

	private ArrayUtil() {}

	/**
	 * shuffle the array in place using Durstenfeld / Fisher-Yates
	 */
	public static void shuffle(Object[] array, RandomSource random) {
		for (int i = array.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			Object temp = array[j];
			array[j] = array[i];
			array[i] = temp;
		}
	}

	/**
	 * shuffle the array in place using Durstenfeld / Fisher-Yates
	 */
	public static void shuffle(int[] array, RandomSource random) {
		for (int i = array.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int temp = array[j];
			array[j] = array[i];
			array[i] = temp;
		}
	}

	/**
	 * shuffle the array in place using Durstenfeld / Fisher-Yates
	 */
	public static void shuffle(float[] array, RandomSource random) {
		for (int i = array.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			float temp = array[j];
			array[j] = array[i];
			array[i] = temp;
		}
	}

	/**
	 * shuffle the array in place using Durstenfeld / Fisher-Yates
	 */
	public static void shuffle(double[] array, RandomSource random) {
		for (int i = array.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			double temp = array[j];
			array[j] = array[i];
			array[i] = temp;
		}
	}

	public static <I,O> O[] cast(I[] input,O[] output) {
		O[] ret = output.clone();
		try {
			for (int i=0;i<Math.min(input.length,output.length);i++) {
				//noinspection unchecked
				ret[i] = (O)output.getClass().arrayType().cast(input[i]);
			}
		} catch(ClassCastException e) {
			BiomancyMod.LOGGER.error("Cannot cast \"{}\" to \"{}\"! This WILL cause issues!\nException:{}", input.getClass().getName(), output.getClass().getName(), e.getLocalizedMessage());
		}
		return ret;
	}

}

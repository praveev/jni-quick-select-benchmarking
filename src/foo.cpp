#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <jni.h>
#include <chrono>
#include <iostream>
#include "QSBenchmark.h"

/**
 *  Generic Partition function.
 *  @param addr - Address of the array/buffer
 *  @param lo - starting index
 *  @param hi - end index
 *
 *  @return - pivot value.
 */

jdouble totalTime = 0;
jdouble selectTime = 0;
jdouble writeTime = 0;

template <typename T>
jlong JNICALL _partition(T *addr, jint lo, jint hi) {
    jint i = lo;
    jint j = hi + 1; //left and right scan indices
    T v = addr[lo]; //partitioning item value

    while (true) {
      //Scan right, scan left, check for scan complete, and exchange
      while (addr[ ++i] < v) {
        if (i == hi) {
          break;
        }
      }
      while (v < addr[ --j]) {
        if (j == lo) {
          break;
        }
      }
      if (i >= j) {
        break;
      }
      T x = addr[i];
      auto begin = std::chrono::high_resolution_clock::now();
      addr[i] = addr[j];
      addr[j] = x;
      auto end = std::chrono::high_resolution_clock::now();
      writeTime += std::chrono::duration_cast<std::chrono::nanoseconds>(end-begin).count();
    }
    //put v=arr[j] into position with a[lo .. j-1] <= a[j] <= a[j+1 .. hi]
    T x = addr[lo];
    auto begin = std::chrono::high_resolution_clock::now();
    addr[lo] = addr[j];
    addr[j] = x;
    auto end = std::chrono::high_resolution_clock::now();
    writeTime += std::chrono::duration_cast<std::chrono::nanoseconds>(end-begin).count();
    return j;
 }


template <typename T>
jlong JNICALL _select(T* addr, jint lo, jint hi, jint pivot) {
    if (addr == NULL) {
        return -1;
    }

    while (hi > lo) {
      int j = _partition(addr, lo, hi);
      if (j == pivot) {
        return addr[pivot];
      }
      if (j > pivot) {
        hi = j - 1;
      }
      else {
        lo = j + 1;
      }
    }
    return addr[pivot];
}

JNIEXPORT jdouble JNICALL Java_QSBenchmark_quickSelect0
(JNIEnv *env, jobject obj, jclass cls, jlong a, jint lo, jint hi, jint pivot) {

    selectTime = 0;
    writeTime = 0;
    totalTime = 0;

    auto beginTotal = std::chrono::high_resolution_clock::now();
    jlong* addr = (jlong*)a;
    auto begin = std::chrono::high_resolution_clock::now();
    jdouble c = _select(addr, lo, hi, pivot);
    auto end = std::chrono::high_resolution_clock::now();
    selectTime = std::chrono::duration_cast<std::chrono::nanoseconds>(end-begin).count();
    auto endTotal = std::chrono::high_resolution_clock::now();
    totalTime = std::chrono::duration_cast<std::chrono::nanoseconds>(endTotal-beginTotal).count();
    return c;

    printf("\nShould never get here:\n");
    fflush(stdout);

    // Should never get here
    return -1;
}

JNIEXPORT jdouble JNICALL Java_QSBenchmark_selectTime0
  (JNIEnv *, jobject) {
  return selectTime;
  }

JNIEXPORT jdouble JNICALL Java_QSBenchmark_writeTime0
  (JNIEnv *, jobject) {
  return writeTime;
  }

JNIEXPORT jdouble JNICALL Java_QSBenchmark_totalCppTime0
  (JNIEnv *, jobject) {
  return totalTime;
  }
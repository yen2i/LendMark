package com.example.lendmark.ui.chatbot

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.google.android.material.datepicker.CalendarConstraints
import java.util.*

class WeekendBlockValidator() : CalendarConstraints.DateValidator {

    override fun isValid(date: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date

        val day = cal.get(Calendar.DAY_OF_WEEK)

        // 주말(토/일) 선택 불가
        return day != Calendar.SATURDAY && day != Calendar.SUNDAY
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        // 아무것도 저장할 필요 없음
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WeekendBlockValidator> {
        override fun createFromParcel(parcel: Parcel): WeekendBlockValidator {
            return WeekendBlockValidator()
        }

        override fun newArray(size: Int): Array<WeekendBlockValidator?> {
            return arrayOfNulls(size)
        }
    }
}

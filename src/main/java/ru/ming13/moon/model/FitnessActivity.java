/*
 * Copyright 2015 Artur Dryomov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ming13.moon.model;

import android.os.Parcel;
import android.os.Parcelable;

public enum FitnessActivity implements Parcelable
{
	WALKING, RUNNING, BIKING;

	public static final Creator<FitnessActivity> CREATOR = new Creator<FitnessActivity>() {
		@Override
		public FitnessActivity createFromParcel(Parcel parcel) {
			return FitnessActivity.valueOf(parcel.readString());
		}

		@Override
		public FitnessActivity[] newArray(int size) {
			return new FitnessActivity[size];
		}
	};

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(this.name());
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
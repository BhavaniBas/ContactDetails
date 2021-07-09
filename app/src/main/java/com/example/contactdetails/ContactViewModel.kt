package com.example.contactdetails

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ContactViewModel : ViewModel() {

    val mutableLiveData = MutableLiveData<List<Contact>>()

    fun getMutableLiveContactData(contactArray: List<Contact>?) {
        if (contactArray != null && contactArray.isNotEmpty()) {
            mutableLiveData.value = contactArray
        }
    }
}
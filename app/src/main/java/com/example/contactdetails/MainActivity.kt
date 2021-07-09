package com.example.contactdetails

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactdetails.databinding.ActivityMainBinding
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var activityMainBinding: ActivityMainBinding? = null
    private var actionMode: ActionMode? = null
    private var contactViewModel: ContactViewModel? = null
    private var contactDataAdapter: ContactDataAdapter? = null
    private var contactArray: List<Contact>? = null
    private var contactList: List<Contact>? = null
    private var actionCallback: ActionCallback? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(activityMainBinding?.toolbar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            supportActionBar?.title = getString(R.string.contact_details)
        }

        // bind RecyclerView
        setAdapter()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            contactArray = getAllContacts(this@MainActivity)
        } else {
            requestPermission()
        }

        if (contactArray != null && contactArray!!.isNotEmpty()) {
            activityMainBinding?.progressBar?.visibility = View.VISIBLE
            recyclerView!!.visibility = View.GONE
            contactViewModel!!.getMutableLiveContactData(contactArray)
        }

        activityMainBinding?.ivAdd?.setOnClickListener { showDialog() }


        // Delay for Recyclerview Items Its not a mandatory ..
        Handler(Looper.getMainLooper()).postDelayed({
            contactViewModel!!.mutableLiveData.observe(
                this@MainActivity,
                { contactItems ->
                    activityMainBinding?.progressBar?.visibility = View.GONE
                    recyclerView!!.visibility = View.VISIBLE
                    if (contactItems != null && contactItems.isNotEmpty()) contactDataAdapter!!.setContactList(
                        contactItems as ArrayList<Contact>
                    )
                })

        }, 100)

        contactDataAdapter!!.setItemClick(object : ContactDataAdapter.OnItemClick {
            override fun onItemClick(view: View?, inbox: Contact?, position: Int) {
                if (contactDataAdapter!!.selectedItemCount() > 0) {
                    toggleActionBar(position)
                } else {
                    Toast.makeText(this@MainActivity, "clicked " + inbox?.name, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onLongPress(view: View?, inbox: Contact?, position: Int) {
                toggleActionBar(position)
            }
        })
    }

    private fun toggleActionBar(position: Int) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionCallback!!)
        }
        toggleSelection(position)
    }

    private fun toggleSelection(position: Int) {
        contactDataAdapter!!.toggleSelection(position)
        val count = contactDataAdapter!!.selectedItemCount()
        if (count == 0) {
            actionMode!!.finish()
        } else {
            actionMode!!.title = count.toString()
            actionMode!!.invalidate()
        }
    }


    private fun setAdapter() {
        actionCallback = ActionCallback()
        recyclerView = activityMainBinding?.contactList as RecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(this@MainActivity)
        recyclerView!!.setHasFixedSize(true)
        activityMainBinding?.progressBar?.visibility = View.VISIBLE
        recyclerView!!.visibility = View.GONE
        contactViewModel =
            ViewModelProviders.of(this@MainActivity).get(ContactViewModel::class.java)
        contactDataAdapter = ContactDataAdapter()
        recyclerView!!.adapter = contactDataAdapter
    }


    private fun getAllContacts(ctx: Context): MutableList<Contact> {
        val list: MutableList<Contact> = ArrayList()
        val contentResolver = ctx.contentResolver
        val cursor =
            contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    val cursorInfo = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )
                    val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                        ctx.contentResolver,
                        ContentUris.withAppendedId(
                            ContactsContract.Contacts.CONTENT_URI,
                            id.toLong()
                        )
                    )
                    val person =
                        ContentUris.withAppendedId(
                            ContactsContract.Contacts.CONTENT_URI,
                            id.toLong()
                        )
                    val pURI =
                        Uri.withAppendedPath(
                            person,
                            ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
                        )
                    var photo: Bitmap? = null
                    if (inputStream != null) {
                        photo = BitmapFactory.decodeStream(inputStream)
                    }
                    while (cursorInfo!!.moveToNext()) {
                        val info = Contact(
                            cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
                            cursorInfo.getString(cursorInfo.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)),
                            cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                                .substring(0, 1)
                        )

                        list.add(info)
                    }
                    // duplicates can be removed ...
                    val hashSet = HashSet(list)
                    list.clear()
                    list.addAll(hashSet)

                    // Sorting the list
                    list.sortWith(Comparator { lhs, rhs -> rhs?.name?.let { lhs?.name?.compareTo(it) }!! })
                    cursorInfo.close()
                }
            }
            cursor.close()
        }
        return list
    }

    private inner class ActionCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            contactDataAdapter?.clearSelection()
            actionMode = null
            Util().toggleStatusBarColor(this@MainActivity, R.color.colorPrimary)
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_CONTACTS
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_CONTACTS
                    )
                ) {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle("Read Contacts permission")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setMessage("Please enable access to contacts.")
                    builder.setOnDismissListener {
                        @TargetApi(Build.VERSION_CODES.M)
                        fun onDismiss(dialog: DialogInterface?) {
                            requestPermissions(
                                arrayOf(Manifest.permission.READ_CONTACTS),
                                PERMISSIONS_REQUEST_READ_CONTACTS
                            )
                        }
                    }
                    builder.show()
                } else {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.READ_CONTACTS),
                        PERMISSIONS_REQUEST_READ_CONTACTS
                    )
                }
            } else {
                contactArray = getAllContacts(this@MainActivity)

                if (contactArray != null && contactArray!!.isNotEmpty()) {
                    activityMainBinding?.progressBar?.visibility = View.VISIBLE
                    recyclerView!!.visibility = View.GONE
                    contactViewModel!!.getMutableLiveContactData(contactArray)
                }
            }
        } else {
            contactArray = getAllContacts(this@MainActivity)

            if (contactArray != null && contactArray!!.isNotEmpty()) {
                activityMainBinding?.progressBar?.visibility = View.VISIBLE
                recyclerView!!.visibility = View.GONE
                contactViewModel!!.getMutableLiveContactData(contactArray)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    contactArray = getAllContacts(this@MainActivity)

                    if (contactArray != null && contactArray!!.isNotEmpty()) {
                        activityMainBinding?.progressBar?.visibility = View.VISIBLE
                        recyclerView!!.visibility = View.GONE
                        contactViewModel!!.getMutableLiveContactData(contactArray)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "You have disabled a contacts permission",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }


    companion object {
        const val REQUEST_READ_CONTACTS = 79
        const val PERMISSIONS_REQUEST_READ_CONTACTS = 79
    }

    private fun showDialog() {
        val commonDialog = Dialog(this)
        commonDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        commonDialog.setCancelable(false)
        commonDialog.setCanceledOnTouchOutside(false)
        commonDialog.setContentView(R.layout.layout_custom_dialog)
        val edName = commonDialog.findViewById<EditText>(R.id.edName)
        val edNumber = commonDialog.findViewById<EditText>(R.id.edContactNumber)
        val btnCancel = commonDialog.findViewById<Button>(R.id.btnCancel)
        val btnOk = commonDialog.findViewById<Button>(R.id.btnOk)
        btnCancel.setOnClickListener { commonDialog.dismiss() }
        btnOk.setOnClickListener {
            closeKeyboard(this)
            when {
                edName.text.toString().isEmpty() -> {
                    Toast.makeText(
                        this,
                        "Contact Name field is empty, Please enter the valid Name",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                edNumber.text.isEmpty() -> {
                    Toast.makeText(
                        this,
                        "Contact Number field is empty, Please enter the valid Number",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    commonDialog.dismiss()
                    if (edName.text.toString().isNotEmpty() && edNumber.text.toString()
                            .isNotEmpty()
                    ) {
                        val addInfoContact = Contact(
                            edName.text.toString(),
                            edNumber.text.toString(),
                            edName.text.toString().substring(0, 1)
                        )
                        val list1 = ArrayList<Contact>()
                        list1.add(addInfoContact)
                        contactArray?.let { list1.addAll(it) }
                        contactDataAdapter?.setContactList(list1)
                        contactDataAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }
        commonDialog.show()
    }



    private fun closeKeyboard(activity: MainActivity) {

        val view: View = activity.findViewById(android.R.id.content)
        val imm: InputMethodManager =
            activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

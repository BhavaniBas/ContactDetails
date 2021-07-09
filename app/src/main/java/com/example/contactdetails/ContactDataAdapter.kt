package com.example.contactdetails

import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.contactdetails.databinding.ContactListItemBinding
import java.util.*

class ContactDataAdapter : RecyclerView.Adapter<ContactDataAdapter.ContactViewHolder?>() {

    private var contactArrayList: ArrayList<Contact>? = null
    private val selectedItems: SparseBooleanArray = SparseBooleanArray()
    private var selectedIndex = -1
    private var itemClick: OnItemClick? = null
    fun setItemClick(itemClick: OnItemClick?) {
        this.itemClick = itemClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val contactListItemBinding: ContactListItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.contact_list_item, parent, false
        )
        return ContactViewHolder(contactListItemBinding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val currentContact = contactArrayList!![position]
        holder.contactListItemBinding.contact = currentContact

        //Changes the activated state of this view.
        holder.contactListItemBinding.lytParent.isActivated = selectedItems[position, false]
        holder.contactListItemBinding.lytParent.setOnClickListener(View.OnClickListener { view ->
            if (itemClick == null) return@OnClickListener
            itemClick!!.onItemClick(view, contactArrayList!![position], position)
        })
        holder.contactListItemBinding.lytParent.setOnLongClickListener { view ->
            if (itemClick == null) {
                false
            } else {
                itemClick!!.onLongPress(view, contactArrayList!![position], position)
                true
            }
        }
        toggleIcon(holder.contactListItemBinding, position)
    }

    /*
      This method will trigger when we we long press the item and it will change the icon of the item to check icon.
    */
    private fun toggleIcon(bi: ContactListItemBinding, position: Int) {
        if (selectedItems[position, false]) {
            bi.lytImage.visibility = View.GONE
            bi.lytChecked.visibility = View.VISIBLE
            if (selectedIndex == position) selectedIndex = -1
        } else {
            bi.lytImage.visibility = View.VISIBLE
            bi.lytChecked.visibility = View.GONE
            if (selectedIndex == position) selectedIndex = -1
        }
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    inner class ContactViewHolder(val contactListItemBinding: ContactListItemBinding) :
        RecyclerView.ViewHolder(contactListItemBinding.root) {

    }

    fun selectedItemCount(): Int {
        return selectedItems.size()
    }

    fun setContactList(contacts: ArrayList<Contact>?) {
        contactArrayList = contacts
        notifyDataSetChanged()
    }

    fun toggleSelection(position: Int) {
        selectedIndex = position
        if (selectedItems[position, false]) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    interface OnItemClick {
        fun onItemClick(view: View?, inbox: Contact?, position: Int)
        fun onLongPress(view: View?, inbox: Contact?, position: Int)
    }

    override fun getItemCount(): Int {
        return if (contactArrayList != null) {
            contactArrayList!!.size
        } else {
            0
        }
    }
}

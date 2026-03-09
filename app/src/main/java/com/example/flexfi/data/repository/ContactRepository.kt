package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.ContactDao
import com.example.flexfi.data.local.entities.ContactEntity
import com.example.flexfi.data.remote.FirestoreUserService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ContactRepository(
    private val contactDao: ContactDao,
    private val firestoreService: FirestoreUserService
) {
    fun getAllContacts(): Flow<List<ContactEntity>> = contactDao.getAllContacts()

    suspend fun addContact(name: String, phone: String, createdBy: String) {
        // Spec §14: if phone already exists, update name instead of creating duplicate
        val existing = contactDao.getContactByPhone(phone)
        if (existing != null) {
            contactDao.updateContact(existing.copy(name = name))
            return
        }

        // Search Firestore if user exists (ghost detection)
        val firebaseUser = firestoreService.findUserByPhone(phone)

        val contact = if (firebaseUser != null) {
            ContactEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                createdBy = createdBy, // phone number of logged-in user
                isGhost = false,
                linkedUserId = firebaseUser.id,
                createdAt = System.currentTimeMillis()
            )
        } else {
            ContactEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                createdBy = createdBy, // phone number of logged-in user
                isGhost = true,
                linkedUserId = null,
                createdAt = System.currentTimeMillis()
            )
        }

        contactDao.insertContact(contact)
    }

    suspend fun syncAllContacts() {
        val allContacts = contactDao.getAllContactsSync()
        val ghosts = allContacts.filter { it.isGhost }

        ghosts.forEach { ghost ->
            val firebaseUser = firestoreService.findUserByPhone(ghost.phone)
            if (firebaseUser != null) {
                val updatedContact = ghost.copy(
                    isGhost = false,
                    linkedUserId = firebaseUser.id
                )
                contactDao.updateContact(updatedContact)
            }
        }
    }

    suspend fun syncContactStatus(phone: String) {
        val existingContact = contactDao.getContactByPhone(phone) ?: return
        if (existingContact.isGhost) {
            val firebaseUser = firestoreService.findUserByPhone(phone)
            if (firebaseUser != null) {
                val updatedContact = existingContact.copy(
                    isGhost = false,
                    linkedUserId = firebaseUser.id
                )
                contactDao.updateContact(updatedContact)
            }
        }
    }

    suspend fun deleteContact(contactId: String) {
        contactDao.deleteContact(contactId)
    }
}

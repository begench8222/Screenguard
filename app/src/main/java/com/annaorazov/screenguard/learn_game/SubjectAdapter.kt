package com.annaorazov.screenguard.learn_game

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.annaorazov.screenguard.R

class SubjectAdapter(
    private val subjects: List<Subject>, // Now using Subject data class instead of raw strings
    private val context: Context,
    private val classLevel: Int
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjects[position]
        holder.subjectName.text = subject.displayName
        holder.subjectIcon.setImageResource(subject.getIconResId())

        holder.subj.setOnClickListener {
            val intent = Intent(context, QuestionMapActivity::class.java).apply {
                putExtra("subject", subject.displayName) // Still pass display name for compatibility
                putExtra("subjectKey", subject.key) // Pass the key for better identification
                putExtra("classLevel", classLevel)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = subjects.size

    class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subjectIcon: ImageView = itemView.findViewById(R.id.subject_icon)
        val subjectName: TextView = itemView.findViewById(R.id.subjectName)
        val subj: View = itemView.findViewById(R.id.subj)
    }
}
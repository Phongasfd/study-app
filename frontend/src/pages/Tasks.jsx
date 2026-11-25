import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import {
  getTasks,
  getTaskSummary,
  createTask,
  updateTask,
  changeTaskStatus,
  deleteTask,
  getSubjects,
} from '../lib/api';
import './Tasks.css';

const EMPTY_FORM = {
  title: '',
  notes: '',
  subjectId: '',
  priority: 'MEDIUM',
  status: 'TODO',
  dueDate: '',
  estimatedMinutes: '',
};

const Tasks = () => {
  const { t } = useTranslation();
  const { user } = useAuth();

  const [tasks, setTasks] = useState([]);
  const [summary, setSummary] = useState({ todo: 0, inProgress: 0, done: 0, overdue: 0, dueToday: 0 });
  const [subjects, setSubjects] = useState([]);

  const [statusFilter, setStatusFilter] = useState('');
  const [dueFilter, setDueFilter] = useState('');
  const [subjectFilter, setSubjectFilter] = useState('');

  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const loadAll = async () => {
    if (!user) return;
    try {
      const params = {};
      if (statusFilter) params.status = statusFilter;
      if (dueFilter) params.due = dueFilter;
      if (subjectFilter) params.subjectId = subjectFilter;

      const [taskList, sum] = await Promise.all([getTasks(params), getTaskSummary()]);
      setTasks(taskList || []);
      if (sum) setSummary(sum);
    } catch (err) {
      console.error('Failed to load tasks:', err);
    }
  };

  useEffect(() => {
    if (user) {
      getSubjects().then((s) => setSubjects(s || [])).catch(() => {});
    }
  }, [user]);

  useEffect(() => {
    loadAll();
  }, [user, statusFilter, dueFilter, subjectFilter]);

  const openCreateModal = () => {
    if (!user) {
      window.location.href = '/login';
      return;
    }
    setEditingId(null);
    setForm(EMPTY_FORM);
    setShowModal(true);
  };

  const openEditModal = (task) => {
    setEditingId(task.id);
    setForm({
      title: task.title || '',
      notes: task.notes || '',
      subjectId: task.subjectId || '',
      priority: task.priority || 'MEDIUM',
      status: task.status || 'TODO',
      dueDate: task.dueDate || '',
      estimatedMinutes: task.estimatedMinutes ? String(task.estimatedMinutes) : '',
    });
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!form.title.trim()) return;
    setSaving(true);
    try {
      const payload = {
        title: form.title.trim(),
        notes: form.notes.trim() || null,
        subjectId: form.subjectId || null,
        priority: form.priority,
        status: form.status,
        dueDate: form.dueDate || null,
        estimatedMinutes: form.estimatedMinutes ? Number(form.estimatedMinutes) : null,
      };
      if (editingId) {
        await updateTask(editingId, payload);
      } else {
        await createTask(payload);
      }
      setShowModal(false);
      await loadAll();
    } catch (err) {
      alert(err.message || 'Failed to save task');
    } finally {
      setSaving(false);
    }
  };

  const handleQuickStatus = async (task, nextStatus) => {
    try {
      await changeTaskStatus(task.id, nextStatus);
      await loadAll();
    } catch (err) {
      console.error('Failed to update task status:', err);
    }
  };

  const handleRemove = async (id) => {
    try {
      await deleteTask(id);
      await loadAll();
    } catch (err) {
      console.error('Failed to delete task:', err);
    }
  };

  return (
    <div className="tasks-page">
      <header className="tasks-header">
        <div>
          <h1 className="h2 text-primary">{t('tasks.title')}</h1>
          <p className="body-md text-on-surface-variant">{t('tasks.subtitle')}</p>
        </div>
        <button className="btn btn-primary" onClick={openCreateModal}>
          <span className="material-symbols-outlined">add</span>
          {t('tasks.newTask')}
        </button>
      </header>

      {/* Summary Strip */}
      <section className="tasks-summary-grid">
        <div className="task-stat-card">
          <span className="material-symbols-outlined stat-icon todo">radio_button_unchecked</span>
          <div>
            <div className="stat-val">{summary.todo}</div>
            <div className="stat-lbl">{t('tasks.statusTODO')}</div>
          </div>
        </div>
        <div className="task-stat-card">
          <span className="material-symbols-outlined stat-icon progress">pending</span>
          <div>
            <div className="stat-val">{summary.inProgress}</div>
            <div className="stat-lbl">{t('tasks.statusIN_PROGRESS')}</div>
          </div>
        </div>
        <div className="task-stat-card">
          <span className="material-symbols-outlined stat-icon done">check_circle</span>
          <div>
            <div className="stat-val">{summary.done}</div>
            <div className="stat-lbl">{t('tasks.statusDONE')}</div>
          </div>
        </div>
        <div className="task-stat-card">
          <span className="material-symbols-outlined stat-icon overdue">warning</span>
          <div>
            <div className="stat-val">{summary.overdue}</div>
            <div className="stat-lbl">{t('tasks.filterOverdue')}</div>
          </div>
        </div>
      </section>

      {/* Filters */}
      <section className="tasks-filter-bar">
        <div className="filter-chips">
          {[
            { value: '', label: t('tasks.filterAll') },
            { value: 'overdue', label: t('tasks.filterOverdue') },
            { value: 'today', label: t('tasks.filterToday') },
            { value: 'week', label: t('tasks.filterWeek') },
          ].map((chip) => (
            <button
              key={chip.value}
              className={`filter-chip ${dueFilter === chip.value ? 'active' : ''}`}
              onClick={() => setDueFilter(chip.value)}
            >
              {chip.label}
            </button>
          ))}
        </div>

        <div className="filter-selects">
          <select
            className="filter-select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">{t('tasks.status')}: {t('tasks.filterAll')}</option>
            <option value="TODO">{t('tasks.statusTODO')}</option>
            <option value="IN_PROGRESS">{t('tasks.statusIN_PROGRESS')}</option>
            <option value="DONE">{t('tasks.statusDONE')}</option>
          </select>

          <select
            className="filter-select"
            value={subjectFilter}
            onChange={(e) => setSubjectFilter(e.target.value)}
          >
            <option value="">{t('tasks.subject')}: {t('tasks.filterAll')}</option>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>
      </section>

      {/* Task List */}
      <section className="tasks-list">
        {tasks.length === 0 ? (
          <div className="tasks-empty-card">{t('tasks.empty')}</div>
        ) : (
          tasks.map((task) => {
            const isDone = task.status === 'DONE';
            return (
              <div key={task.id} className={`task-row-card ${isDone ? 'task-done' : ''}`}>
                <button
                  className={`task-check-btn ${isDone ? 'checked' : ''}`}
                  onClick={() => handleQuickStatus(task, isDone ? 'TODO' : 'DONE')}
                  title={isDone ? t('tasks.statusTODO') : t('tasks.statusDONE')}
                >
                  <span className="material-symbols-outlined">
                    {isDone ? 'check_circle' : 'radio_button_unchecked'}
                  </span>
                </button>

                <div className="task-main" onClick={() => openEditModal(task)}>
                  <div className="task-title-row">
                    <span className="task-title-text">{task.title}</span>
                    <span className={`priority-pill priority-${task.priority}`}>
                      {t(`tasks.priority${task.priority}`)}
                    </span>
                    {task.subjectName && (
                      <span className="subject-pill">{task.subjectName}</span>
                    )}
                    {task.overdue && (
                      <span className="overdue-pill">{t('tasks.overdueTag')}</span>
                    )}
                  </div>
                  {task.notes && <p className="task-notes-text">{task.notes}</p>}
                  <div className="task-meta-row">
                    {task.dueDate && (
                      <span className="task-meta-item">
                        <span className="material-symbols-outlined">event</span>
                        {task.dueDate}
                      </span>
                    )}
                    {task.estimatedMinutes && (
                      <span className="task-meta-item">
                        <span className="material-symbols-outlined">schedule</span>
                        {task.estimatedMinutes}m
                      </span>
                    )}
                  </div>
                </div>

                <div className="task-actions">
                  <select
                    className="task-status-select"
                    value={task.status}
                    onChange={(e) => handleQuickStatus(task, e.target.value)}
                  >
                    <option value="TODO">{t('tasks.statusTODO')}</option>
                    <option value="IN_PROGRESS">{t('tasks.statusIN_PROGRESS')}</option>
                    <option value="DONE">{t('tasks.statusDONE')}</option>
                  </select>
                  <button className="task-icon-btn" onClick={() => handleRemove(task.id)}>
                    <span className="material-symbols-outlined">delete</span>
                  </button>
                </div>
              </div>
            );
          })
        )}
      </section>

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="h3 text-primary">
                {editingId ? t('tasks.editTask') : t('tasks.newTask')}
              </h3>
              <button className="modal-close-btn" onClick={() => setShowModal(false)}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <form onSubmit={handleSave} className="modal-form">
              <div className="form-group">
                <label className="label-sm">{t('tasks.taskTitle')} *</label>
                <input
                  type="text"
                  className="form-input"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  placeholder={t('tasks.taskTitlePlaceholder')}
                  required
                />
              </div>

              <div className="form-group">
                <label className="label-sm">{t('tasks.notes')}</label>
                <textarea
                  className="form-input"
                  rows={3}
                  value={form.notes}
                  onChange={(e) => setForm({ ...form, notes: e.target.value })}
                  placeholder={t('tasks.notesPlaceholder')}
                />
              </div>

              <div className="form-row-2">
                <div className="form-group">
                  <label className="label-sm">{t('tasks.subject')}</label>
                  <select
                    className="form-input"
                    value={form.subjectId}
                    onChange={(e) => setForm({ ...form, subjectId: e.target.value })}
                  >
                    <option value="">{t('tasks.noSubject')}</option>
                    {subjects.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="label-sm">{t('tasks.priority')}</label>
                  <select
                    className="form-input"
                    value={form.priority}
                    onChange={(e) => setForm({ ...form, priority: e.target.value })}
                  >
                    <option value="LOW">{t('tasks.priorityLOW')}</option>
                    <option value="MEDIUM">{t('tasks.priorityMEDIUM')}</option>
                    <option value="HIGH">{t('tasks.priorityHIGH')}</option>
                  </select>
                </div>
              </div>

              <div className="form-row-2">
                <div className="form-group">
                  <label className="label-sm">{t('tasks.dueDate')}</label>
                  <input
                    type="date"
                    className="form-input"
                    value={form.dueDate}
                    onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="label-sm">{t('tasks.estimatedMinutes')}</label>
                  <input
                    type="number"
                    min="1"
                    max="10000"
                    className="form-input"
                    value={form.estimatedMinutes}
                    onChange={(e) => setForm({ ...form, estimatedMinutes: e.target.value })}
                    placeholder="45"
                  />
                </div>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                  {t('auth.cancel')}
                </button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {t('tasks.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Tasks;

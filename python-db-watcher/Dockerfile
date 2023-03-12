FROM registry.access.redhat.com/ubi8/python-39:latest

# Add application sources with correct permissions for OpenShift
ADD --chown=1001 app-src .
USER 1001

# Install the dependencies
# RUN pip install -U "pip>=19.3.1" && \
#    pip install -r requirements.txt && \
#    python manage.py collectstatic --noinput && \
#    python manage.py migrate

# Run the application
CMD python app.py

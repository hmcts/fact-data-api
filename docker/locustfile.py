from locust import FastHttpUser, TaskSet, task, between
import random
import string

class AdminThread(TaskSet):
  @task
  def rl_bucket4j_admin(self):
    headers = {
      "Rate-Limit-Identifier":self.user.identifier
    }
    self.client.request(method = "GET", url = "/rl-general/b4j-admin", headers = headers)

class PublicThread(TaskSet):
  @task
  def rl_bucket4j_public(self):
    headers = {
      "Rate-Limit-Identifier":self.user.identifier
    }
    for _ in range(self.user.call_count):
      self.client.request(method = "GET", url = "/rl-general/b4j-public", headers = headers)

# makes 1 request every 2 to 8 seconds
class NormalUser(FastHttpUser):
  weight = 100
  wait_time = between(2, 8)
  call_count = 1
  tasks = {
    PublicThread:10,
    AdminThread:1
  }
  identifier = "normal"

  def on_start(self):
    length = 8
    self.identifier += ''.join(random.choices(string.ascii_letters + string.digits, k=length))

# makes 5 requests every 2 to 4 seconds
class AggressiveUser(FastHttpUser):
  weight = 10
  wait_time = between(2, 4)
  call_count = 5
  tasks = {
    PublicThread:20,
    AdminThread:1
  }
  identifier = "aggressive"

  def on_start(self):
    length = 8
    self.identifier += ''.join(random.choices(string.ascii_letters + string.digits, k=length))

# makes 400 requests every 1 to 2 minutes
class HackerUser(FastHttpUser):
  weight = 1
  wait_time = between(60, 120)
  call_count = 400
  tasks = {
    PublicThread
  }
  identifier = "hacker"

  def on_start(self):
    length = 8
    self.identifier += ''.join(random.choices(string.ascii_letters + string.digits, k=length))

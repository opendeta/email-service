name: Email Notification System

on:
  push:

jobs:
  send_email:
    runs-on: ubuntu-latest

    steps:
      - name: Send Email
        uses: dawidd6/action-send-mail@v3
        with:
          server_address: smtp.gmail.com
          server_port: 465
          secure: true
          username: ${{ secrets.EMAIL_USER }}
          password: ${{ secrets.EMAIL_PASS }}
          subject: "🚀 GitHub Update Alert"
          to: ${{ secrets.EMAIL_TO }}
          from: ${{ secrets.EMAIL_USER }}
          body: "তোমার GitHub repo-তে নতুন update হয়েছে 😎📩"
